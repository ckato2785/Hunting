package com.example.hunts.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Sparrow(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val bitmap: Bitmap,
    private val speed: Float // ⭐ 6.4 스테이지 난이도를 위해 추가된 속도 인자
) : GameObject {

    // x, y는 참새 이미지의 중심 좌표가 됩니다.
    private var x: Float
    private var y: Float

    // private val speed = 500f // ⬅️ 스테이지 데이터에서 받은 speed 인자로 대체되었으므로, 이 고정 값은 제거합니다.
    private var angle: Float

    // Bitmap의 크기를 사용하여 충돌 판정 크기로 사용
    private val width = bitmap.width.toFloat()
    private val height = bitmap.height.toFloat()

    init {
        // 화면 외곽 4면 중 하나를 랜덤으로 선택하여 스폰 로직
        val side = Random.nextInt(4)

        when (side) {
            0 -> { // 위쪽 외곽
                x = Random.nextFloat() * screenWidth
                y = -height / 2
                angle = Random.nextFloat() * Math.PI.toFloat() // 아래로 향하게
            }
            1 -> { // 오른쪽 외곽
                x = screenWidth + width / 2
                y = Random.nextFloat() * screenHeight
                angle = Random.nextFloat() * Math.PI.toFloat() + Math.PI.toFloat() / 2 // 왼쪽으로 향하게
            }
            2 -> { // 아래쪽 외곽
                x = Random.nextFloat() * screenWidth
                y = screenHeight + height / 2
                angle = Random.nextFloat() * Math.PI.toFloat() + Math.PI.toFloat() // 위로 향하게
            }
            else -> { // 왼쪽 외곽
                x = -width / 2
                y = Random.nextFloat() * screenHeight
                angle = Random.nextFloat() * Math.PI.toFloat() - Math.PI.toFloat() / 2 // 오른쪽으로 향하게
            }
        }
        // 각도를 화면 중앙 쪽으로 조정하는 로직은 현재 생략
    }

    override fun update(deltaTime: Float) {
        // (속도 * cos(각도)) * 델타타임 -> x축 이동
        // ⭐ 생성자에서 받은 speed 변수를 사용합니다.
        x += (speed * cos(angle)) * deltaTime
        // (속도 * sin(각도)) * 델타타임 -> y축 이동
        y += (speed * sin(angle)) * deltaTime
    }

    override fun draw(canvas: Canvas) {
        // x, y가 중심 좌표이므로, 왼쪽 상단 좌표를 계산하여 그립니다.
        val left = x - width / 2
        val top = y - height / 2

        canvas.drawBitmap(bitmap, left, top, null)
    }

    override fun checkHit(touchX: Float, touchY: Float): Boolean {
        // 터치 좌표가 이미지 직사각형 영역 안에 있는지 확인
        val left = x - width / 2
        val top = y - height / 2
        val right = x + width / 2
        val bottom = y + height / 2

        return touchX >= left && touchX <= right && touchY >= top && touchY <= bottom
    }

    override fun isOffScreen(): Boolean {
        // 이미지 크기를 고려하여 화면 밖 확인
        return x < -width || x > screenWidth + width ||
                y < -height || y > screenHeight + height
    }

    override fun getScoreValue(): Int {
        return 5
    }
}