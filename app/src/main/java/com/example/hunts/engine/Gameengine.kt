package com.example.hunts.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

enum class GameState {
    RUNNING, PAUSED, END
}

class GameEngine(
    val screenWidth: Int,
    val screenHeight: Int,
    private val sparrowBitmap: Bitmap // Sparrow 객체 생성 시 전달할 Bitmap
) {

    private val activeObjects = mutableListOf<GameObject>()
    var gameState: GameState = GameState.RUNNING
    private var lastUpdateTime = System.currentTimeMillis()

    // 터치 이벤트 처리를 위한 콜백 함수 (점수 관리자와 연결될 부분)
    var onObjectHit: ((GameObject) -> Unit)? = null

    // 임시 스폰 타이머 변수
    private var spawnTimer = 0f
    private val spawnInterval = 1.5f // 1.5초마다 참새 1마리 스폰

    init {
        // 테스트를 위해 시작하자마자 참새 2마리 스폰
        spawnSparrow()
        spawnSparrow()
    }

    fun update() {
        if (gameState != GameState.RUNNING) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // 1. 임시 스폰 로직
        spawnTimer += deltaTime
        if (spawnTimer >= spawnInterval) {
            spawnSparrow()
            spawnTimer = 0f
        }

        // 2. 모든 활성 오브젝트 업데이트
        for (obj in activeObjects) {
            obj.update(deltaTime)
        }

        // 3. 화면 밖 오브젝트 제거
        activeObjects.removeIf { it.isOffScreen() }
    }

    fun draw(canvas: Canvas) {
        // 1. 배경 그리기 (검은색)
        canvas.drawColor(Color.BLACK)

        // 2. 모든 활성 오브젝트 그리기
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
        if (gameState != GameState.RUNNING) return

        // 겹침 처리를 위해 리스트를 역순으로 순회 (가장 위에 있는 것부터 검사)
        for (i in activeObjects.indices.reversed()) {
            val obj = activeObjects[i]
            if (obj.checkHit(touchX, touchY)) {
                // 1. 히트 콜백 호출 (점수 처리를 위해)
                onObjectHit?.invoke(obj)

                // 2. 명중된 오브젝트를 리스트에서 제거 (화면에서 사라지게 함)
                activeObjects.removeAt(i)

                // 3. 하나만 처리하고 종료
                return
            }
        }
    }

    /**
     * Sparrow 객체를 생성하여 activeObjects 리스트에 추가합니다.
     */
    private fun spawnSparrow() {
        // Sparrow 생성 시, 화면 크기와 Bitmap을 전달합니다.
        val sparrow = Sparrow(screenWidth, screenHeight, sparrowBitmap)
        activeObjects.add(sparrow)
    }
}