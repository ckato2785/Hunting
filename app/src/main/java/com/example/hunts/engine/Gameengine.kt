package com.example.hunts.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.content.SharedPreferences

enum class GameState {
    RUNNING, PAUSED, END
}

class GameEngine(
    val screenWidth: Int,
    val screenHeight: Int,
    private val sparrowBitmap: Bitmap, // Sparrow 객체 생성 시 전달할 Bitmap
    private val context: Context
) {
    // Stage Management Variables (스테이지 관리 변수)
    var currentStageIndex: Int = 1
    // StageData: loadStage에서 초기화됩니다. (StageManager 클래스가 필요합니다)
    lateinit var currentStageData: StageData

    // ⭐ 2. 클래스 상태 변수 (private set 접근자를 정확히 사용)
    var highestScore: Int = 0
        private set
    var isStageSuccess: Boolean = false
        private set
    var endScreenTimer: Float = 0f
    val FADE_DURATION = 1.0f

    private val activeObjects = mutableListOf<GameObject>()
    var gameState: GameState = GameState.RUNNING
    private var lastUpdateTime = System.currentTimeMillis()

    // ScoreManager 인스턴스
    val scoreManager = ScoreManager()

    var onObjectHit: ((GameObject) -> Unit)? = null

    private var spawnTimer = 0f
    // ⭐ spawnInterval 대신 currentStageData.spawnInterval을 사용합니다.

    private val PREFS_NAME = "HuntsPrefs"
    private val HIGH_SCORE_KEY = "high_score"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 버튼 영역 변수
    private var restartBounds: Rect? = null
    private var nextStageBounds: Rect? = null


    init {
        this.onObjectHit = { obj ->
            scoreManager.addScore(obj.getScoreValue())
        }
        loadHighestScore()
        // ⭐ 초기화 시 Stage 1을 로드하여 게임을 시작합니다.
        loadStage(1)
    }

    /**
     * 스테이지 상태를 포함한 모든 게임 변수를 초기화합니다.
     */
    private fun resetGameStates() {
        scoreManager.reset() // 점수 및 타이머 초기화
        activeObjects.clear() // 활성 오브젝트 모두 제거
        gameState = GameState.RUNNING // RUNNING 상태로 전환
        isStageSuccess = false
        endScreenTimer = 0f
        lastUpdateTime = System.currentTimeMillis()
        spawnTimer = 0f // 스폰 타이머 초기화
    }

    /**
     * 주어진 인덱스의 스테이지 데이터를 로드하고 게임 상태를 재설정합니다.
     */
    fun loadStage(index: Int) {
        currentStageIndex = index
        // StageManager (별도 구현 필요)를 통해 스테이지 데이터를 가져옵니다.
        currentStageData = StageManager.getStageData(index)

        // ⭐ 핵심 게임 상태 초기화
        resetGameStates()

        // 스테이지별 시간 및 목표 점수 설정
        scoreManager.setStageData(currentStageData.gameDuration, currentStageData.targetScore)

        // 초기 스패로우 스폰
        spawnSparrow()
        spawnSparrow()
    }

    /**
     * 다음 스테이지를 로드합니다. 마지막 스테이지일 경우 Stage 1로 돌아갑니다.
     */
    fun loadNextStage() {
        if (currentStageIndex < StageManager.totalStages) {
            loadStage(currentStageIndex + 1)
        } else {
            // 마지막 스테이지 완료: Stage 1로 돌아가거나 최종 화면 표시 (여기서는 Stage 1로 복귀)
            loadStage(1)
        }
    }

    /**
     * 현재 스테이지를 재시작합니다.
     */
    fun resetCurrentStage() {
        loadStage(currentStageIndex)
    }


    fun update() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // 1. END 상태 처리 (가장 먼저 실행)
        if (gameState == GameState.END) {
            // 게임 오버 상태일 때는 페이드 타이머만 업데이트합니다.
            if (endScreenTimer < FADE_DURATION) {
                endScreenTimer += deltaTime
            }
            return // END 상태에서는 추가적인 게임 로직 실행 중단
        }

        // 2. RUNNING 상태일 때 시간 업데이트 및 게임 종료 확인
        if (gameState == GameState.RUNNING) {

            scoreManager.updateTime(deltaTime)

            if (scoreManager.isTimeUp()) {
                // 게임 종료 로직: END 상태로 전환
                gameState = GameState.END
                activeObjects.clear()
                endScreenTimer = 0f // 타이머 초기화 (페이드 인 시작)
                processGameResult()

                // END 상태로 전환되었으므로, 다음 프레임부터 END 상태 로직 실행
                return
            }
        }

        // PAUSED 상태인 경우 로직 실행 중단
        if (gameState != GameState.RUNNING) return


        // 3. RUNNING 상태일 때 게임 로직 실행

        // ⭐ 스테이지 데이터의 스폰 간격 사용
        spawnTimer += deltaTime
        if (spawnTimer >= currentStageData.spawnInterval) {
            spawnSparrow()
            spawnTimer = 0f
        }

        // 모든 활성 오브젝트 업데이트
        for (obj in activeObjects) {
            obj.update(deltaTime)
        }

        // 화면 밖 오브젝트 제거
        activeObjects.removeAll { it.isOffScreen() }
    }


    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        for (obj in activeObjects) {
            obj.draw(canvas)
        }
    }

    fun addObject(obj: GameObject) {
        activeObjects.add(obj)
    }

    /**
     * 터치 이벤트 처리: 터치 좌표에 오브젝트가 있는지 확인하고 처리합니다.
     */
    fun handleTouch(touchX: Float, touchY: Float) {
        // 게임 종료 상태일 경우: 버튼 터치 처리
        if (gameState == GameState.END) {

            // 1. Restart 버튼 (보통 왼쪽 버튼 - 현재 스테이지 재도전)
            if (restartBounds?.contains(touchX.toInt(), touchY.toInt()) == true) {
                resetCurrentStage() // 현재 스테이지 재시작
                return
            }

            // 2. Next Stage/Retry 버튼 (보통 오른쪽 버튼)
            if (nextStageBounds?.contains(touchX.toInt(), touchY.toInt()) == true) {
                if (isStageSuccess) {
                    // 성공: 다음 스테이지로 이동
                    loadNextStage()
                } else {
                    // 실패: 현재 스테이지 재도전
                    resetCurrentStage()
                }
                return
            }
            return
        }

        if (gameState != GameState.RUNNING) return

        // 오브젝트 히트 처리
        for (i in activeObjects.indices.reversed()) {
            val obj = activeObjects[i]
            if (obj.checkHit(touchX, touchY)) {
                onObjectHit?.invoke(obj)
                activeObjects.removeAt(i)
                return
            }
        }
    }

    /**
     * Sparrow 객체를 생성하여 activeObjects 리스트에 추가합니다.
     */
    private fun spawnSparrow() {
        // ⭐ FIX: Sparrow 생성자에 speed 인자를 전달합니다.
        // StageData에서 가져온 속도를 사용합니다.
        val speed = currentStageData.sparrowSpeed
        val sparrow = Sparrow(screenWidth, screenHeight, sparrowBitmap, speed) // speed 인자 전달
        activeObjects.add(sparrow)
    }

    /**
     * 게임 상태를 초기화하고 재시작합니다. (Stage 1 로드)
     */
    fun resetGame() {
        loadStage(1)
    }

    /**
     * 게임 결과 처리 함수: 성공 여부 판단 및 최고 점수 갱신
     */
    private fun processGameResult() {
        // 1. 성공/실패 판단
        isStageSuccess = scoreManager.score >= scoreManager.targetScore

        // 2. 최고 점수 갱신 및 저장 로직
        if (scoreManager.score > highestScore) {
            highestScore = scoreManager.score
            saveHighestScore(highestScore)
        }
    }

    /**
     * GameView에서 버튼 영역을 설정할 함수
     */
    fun setButtonBounds(restart: Rect?, nextStage: Rect?) {
        restartBounds = restart
        nextStageBounds = nextStage
    }

    /**
     * 최고 점수 로드
     */
    private fun loadHighestScore() {
        highestScore = prefs.getInt(HIGH_SCORE_KEY, 0)
    }

    /**
     * 최고 점수 저장
     */
    private fun saveHighestScore(score: Int) {
        prefs.edit().putInt(HIGH_SCORE_KEY, score).apply()
    }
}