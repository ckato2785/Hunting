package com.example.hunts.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import com.example.hunts.model.Bunting
import com.example.hunts.model.Magpie
import com.example.hunts.model.Sparrow
import kotlin.random.Random

/**
 * 게임 상태를 정의합니다.
 */
enum class GameState {
    RUNNING, // 게임이 실행 중
    PAUSED,  // 일시정지
    END,     // 게임 종료 (시간 초과 또는 라이프 0)
    READY    // 게임 시작 대기 상태
}

/**
 * 게임의 핵심 로직 (업데이트 및 그리기)을 담당합니다.
 * @param screenWidth 화면 너비 (픽셀)
 * @param screenHeight 화면 높이 (픽셀)
 * @param birdBitmaps 모든 새 이미지를 담은 맵
 */
class GameEngine(
    val screenWidth: Int,
    val screenHeight: Int,
    private val birdBitmaps: Map<Int, Bitmap>
) {
    /** UI가 페이드 인/아웃 되는 시간 (초) */
    val FADE_DURATION = 0.5f

    // Bird ID (MainActivity에서 사용한 R.drawable ID와 일치해야 합니다.)
    private val SPARROW_ID = 1
    private val BUNTING_ID = 2
    private val MAGPIE_ID = 3


    // 상단 UI 바 높이. 오브젝트 스폰 Y 좌표의 최솟값으로 사용됩니다.
    private var objectStartY: Int = 0

    // 활성화된 모든 게임 오브젝트를 저장하는 리스트
    private val activeObjects = mutableListOf<GameObject>()

    val scoreManager = ScoreManager()
    var currentStageIndex: Int = 1
    var isStageSuccess: Boolean = false
    // 현재 스테이지 데이터를 저장합니다. loadStage 호출 시 초기화됩니다.
    lateinit var currentStageData: StageData

    // UI 및 시간 관리 변수
    var gameState: GameState = GameState.READY
    private var lastUpdateTime = System.currentTimeMillis()
    // 오브젝트 명중 시 호출될 콜백 함수
    var onObjectHit: ((GameObject) -> Unit)? = null
    // 게임 종료 화면에서 터치 입력을 지연시키기 위한 타이머
    var endScreenTimer: Float = 0f
    // 새 스폰 주기 관리를 위한 타이머
    private var spawnTimer = 0f

    // 버튼 영역 (GameView에서 설정)
    private var prevStageButtonBounds: Rect? = null
    private var nextStageButtonBounds: Rect? = null
    private var pauseButtonBounds: Rect? = null

    init {
        this.onObjectHit = { obj ->
            scoreManager.addScore(obj.getScoreValue())
        }
        loadStage(currentStageIndex)
    }

    // =======================================================
    // 1. UI 연동 함수 (상단 UI 높이 설정)
    // =======================================================

    /**
     * GameView에서 상단 UI 바 높이를 설정하여 오브젝트 스폰 Y 좌표의 최솟값을 조정합니다.
     */
    fun setTopUiBarHeight(height: Int) {
        this.objectStartY = height
        Log.d("GameEngine", "Object spawn Y adjusted to: $objectStartY")
    }

    /**
     * GameView에서 버튼 영역을 설정합니다.
     */
    fun setStageButtonBounds(prevBtn: Rect?, nextBtn: Rect?, pauseBtn: Rect? = null) {
        this.prevStageButtonBounds = prevBtn
        this.nextStageButtonBounds = nextBtn
        this.pauseButtonBounds = pauseBtn
    }

    // =======================================================
    // 2. 스테이지 관리
    // =======================================================

    /**
     * 특정 스테이지 번호의 데이터를 로드하고 게임 상태를 초기화합니다.
     */
    fun loadStage(stageIndex: Int) {
        currentStageIndex = stageIndex
        currentStageData = StageManager.getStageData(stageIndex)

        // 스테이지 초기화
        activeObjects.clear()

        // **오류 수정 부분 1:** StageData의 'gameDuration' 속성을 사용하여 시간 초기화
        // StageData 클래스에 gameDuration 속성이 정의되어 있어야 합니다.
        scoreManager.reset(currentStageData.gameDuration)

        gameState = GameState.READY
        spawnTimer = 0f
        endScreenTimer = 0f
        isStageSuccess = false
    }

    // =======================================================
    // 3. 게임 업데이트 (매 프레임)
    // =======================================================

    /**
     * 게임 로직을 업데이트합니다. (물리, 시간, 스폰)
     */
    fun update() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f // 초 단위

        if (gameState == GameState.RUNNING) {
            // RUNNING 상태일 때만 시간을 업데이트하고 deltaTime을 소비합니다.
            lastUpdateTime = currentTime

            // 시간 감소
            scoreManager.updateTime(deltaTime)

            // 스폰 타이머 업데이트 및 새 스폰
            spawnTimer += deltaTime

            // StageData의 spawnInterval 속성 사용
            if (spawnTimer >= currentStageData.spawnInterval) {
                // StageData의 개별 속도 값을 spawnBird에 전달
                spawnBird(
                    currentStageData.sparrowSpeed,
                    currentStageData.buntingSpeed,
                    currentStageData.magpieSpeed
                )
                spawnTimer = 0f
            }

            // 오브젝트 이동 및 경계 처리
            val iterator = activeObjects.iterator()
            while (iterator.hasNext()) {
                val obj = iterator.next()
                obj.update(deltaTime)
                // 화면 밖으로 나가면 제거 (GameObject의 y는 Float 타입)
                if (obj.y > screenHeight) {
                    iterator.remove()
                }
            }

            // 게임 오버 조건 확인 (시간 초과)
            if (scoreManager.remainingTime <= 0f) {
                endGame()
            }

        } else if (gameState == GameState.END) {
            // 게임 종료 화면 타이머 업데이트 (터치 입력 활성화 지연용)
            val endDeltaTime = (currentTime - lastUpdateTime) / 1000f
            endScreenTimer += endDeltaTime
            // END 상태에서는 lastUpdateTime을 현재 시간으로 계속 업데이트하여 deltaTime 누적을 방지합니다.
            lastUpdateTime = currentTime
        } else if (gameState == GameState.READY || gameState == GameState.PAUSED) {
            // READY나 PAUSED 상태에서는 deltaTime 누적 방지를 위해 lastUpdateTime을 계속 재설정합니다.
            lastUpdateTime = currentTime
        }
    }


    /**
     * 게임 종료 시 호출됩니다. 성공/실패 여부를 결정합니다.
     */
    private fun endGame() {
        gameState = GameState.END
        isStageSuccess = scoreManager.score >= currentStageData.targetScore
        Log.d("GameEngine", "Game Ended. Success: $isStageSuccess, Final Score: ${scoreManager.score}")
    }

    // =======================================================
    // 4. 터치 처리
    // =======================================================

    /**
     * 사용자의 터치 입력을 처리합니다.
     * @param x 터치 X 좌표
     * @param y 터치 Y 좌표
     */
    fun handleTouch(x: Float, y: Float) {
        when (gameState) {
            GameState.READY -> {
                // READY 상태에서는 아무 곳이나 터치하면 게임 시작
                gameState = GameState.RUNNING
                // 게임 시작 시 타이머 리셋
                lastUpdateTime = System.currentTimeMillis()
            }

            GameState.RUNNING -> {
                // 일시정지 버튼 처리
                if (pauseButtonBounds?.contains(x.toInt(), y.toInt()) == true) {
                    gameState = GameState.PAUSED
                    return
                }

                // 새 오브젝트 터치 처리
                val iterator = activeObjects.iterator()
                while (iterator.hasNext()) {
                    val obj = iterator.next()
                    if (obj.checkHit(x, y)) {
                        onObjectHit?.invoke(obj)
                        iterator.remove() // 명중한 새 제거
                        return
                    }
                }
            }

            GameState.PAUSED -> {
                // RESUME 버튼 (임시로 prevStageButtonBounds 사용) 처리
                if (prevStageButtonBounds?.contains(x.toInt(), y.toInt()) == true) {
                    gameState = GameState.RUNNING
                    // 일시 정지 해제 시 타이머 리셋
                    lastUpdateTime = System.currentTimeMillis()
                }
            }

            GameState.END -> {
                // END 상태에서는 일정 시간 이후에만 버튼 터치 허용 (깜빡임 방지)
                if (endScreenTimer < FADE_DURATION) return

                // RETRY 버튼 (왼쪽: prevStageButtonBounds) 처리: 현재 스테이지 다시 로드
                if (prevStageButtonBounds?.contains(x.toInt(), y.toInt()) == true) {
                    loadStage(currentStageIndex)
                    gameState = GameState.READY // READY 상태로 재시작 준비
                    return
                }

                // NEXT/RETRY 버튼 (오른쪽: nextStageButtonBounds) 처리
                if (nextStageButtonBounds?.contains(x.toInt(), y.toInt()) == true) {
                    if (isStageSuccess) {
                        // 성공: 다음 스테이지 로드
                        // **오류 수정 부분 2:** StageManager 객체에 hasNextStage 함수가 정의되어 있어야 합니다.
                        if (StageManager.hasNextStage(currentStageIndex)) {
                            loadStage(currentStageIndex + 1)
                        } else {
                            // 마지막 스테이지 성공 시, 현재 스테이지 다시 로드
                            loadStage(currentStageIndex)
                        }
                        gameState = GameState.READY // READY 상태로 재시작 준비
                    } else {
                        // 실패: 현재 스테이지 다시 로드
                        loadStage(currentStageIndex)
                        gameState = GameState.READY // READY 상태로 재시작 준비
                    }
                    return
                }
            }
        }
    }

    // =======================================================
    // 5. 그리기 (GameView에서 호출)
    // =======================================================

    /**
     * 모든 활성화된 게임 오브젝트를 캔버스에 그립니다.
     */
    fun draw(canvas: Canvas) {
        activeObjects.forEach { obj ->
            obj.draw(canvas)
        }
    }

    // =======================================================
    // 6. 스폰 로직
    // =======================================================

    /**
     * Sparrow, Bunting, Magpie 중 하나를 랜덤으로 생성하여 activeObjects 리스트에 추가합니다.
     * 스폰 확률: 참새(Sparrow) 60%, 멧새(Bunting) 25%, 까치(Magpie) 15%
     * @param sparrowSpeed 참새에게 적용할 이동 속도
     * @param buntingSpeed 멧새에게 적용할 이동 속도
     * @param magpieSpeed 까치에게 적용할 이동 속도
     */
    private fun spawnBird(sparrowSpeed: Float, buntingSpeed: Float, magpieSpeed: Float) {
        if (birdBitmaps.isEmpty()) {
            Log.w("GameEngine", "Bird Bitmaps are empty, cannot spawn.")
            return
        }

        val rand = Random.nextInt(100) // 0 ~ 99
        val bird: GameObject = when {
            // 60% 확률로 참새 (Sparrow)
            rand < 60 -> {
                val bitmap = birdBitmaps[SPARROW_ID]!!
                Sparrow(screenWidth, screenHeight, bitmap, sparrowSpeed, objectStartY)
            }
            // 25% 확률로 멧새 (Bunting)
            rand < 85 -> {
                val bitmap = birdBitmaps[BUNTING_ID]!!
                Bunting(screenWidth, screenHeight, bitmap, buntingSpeed, objectStartY)
            }
            // 15% 확률로 까치 (Magpie)
            else -> {
                val bitmap = birdBitmaps[MAGPIE_ID]!!
                Magpie(screenWidth, screenHeight, bitmap, magpieSpeed, objectStartY)
            }
        }
        activeObjects.add(bird)
    }
}