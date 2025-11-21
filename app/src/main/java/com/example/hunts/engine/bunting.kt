package com.example.hunts.model

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.hunts.engine.GameObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 멧새(Bunting) 오브젝트입니다. (클릭 시 -2점)
 */
class Bunting(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val bitmap: Bitmap,
    private val speed: Float,
    private val objectStartY: Int // 상단 UI 바 높이
) : GameObject {

    override var x: Float
    override var y: Float
    private var angle: Float // 이동 각도

    override val width = bitmap.width.toFloat()
    override val height = bitmap.height.toFloat()

    init {
        // 중앙 방향으로 이동하도록 각도 계산
        val offsetAngleRange = Math.PI.toFloat() / 6f

        // 1. 화면 외곽에서 랜덤하게 스폰 위치를 결정합니다.
        val side = Random.nextInt(4)

        when (side) {
            0 -> { // 위쪽 외곽
                x = Random.nextFloat() * screenWidth
                y = objectStartY.toFloat() + height / 2
                angle = Math.PI.toFloat() / 2f + Random.nextFloat() * offsetAngleRange * 2 - offsetAngleRange
            }
            1 -> { // 오른쪽 외곽
                x = screenWidth + width / 2
                y = objectStartY + Random.nextFloat() * (screenHeight - objectStartY)
                angle = Math.PI.toFloat() + Random.nextFloat() * offsetAngleRange * 2 - offsetAngleRange
            }
            2 -> { // 아래쪽 외곽
                x = Random.nextFloat() * screenWidth
                y = screenHeight + height / 2
                angle = -Math.PI.toFloat() / 2f + Random.nextFloat() * offsetAngleRange * 2 - offsetAngleRange
            }
            else -> { // 왼쪽 외곽
                x = -width / 2
                y = objectStartY + Random.nextFloat() * (screenHeight - objectStartY)
                angle = Random.nextFloat() * offsetAngleRange * 2 - offsetAngleRange
            }
        }
    }

    override fun update(deltaTime: Float) {
        x += (speed * cos(angle)) * deltaTime
        y += (speed * sin(angle)) * deltaTime
    }

    override fun draw(canvas: Canvas) {
        val left = x - width / 2
        val top = y - height / 2
        canvas.drawBitmap(bitmap, left, top, null)
    }

    override fun checkHit(touchX: Float, touchY: Float): Boolean {
        val left = x - width / 2
        val top = y - height / 2
        val right = x + width / 2
        val bottom = y + height / 2
        return touchX >= left && touchX <= right && touchY >= top && touchY <= bottom
    }

    override fun isOffScreen(screenHeight: Int): Boolean {
        return x < -width || x > screenWidth + width ||
                y < -height || y > screenHeight + height
    }

    override fun getScoreValue(): Int {
        return -2 // 멧새는 -2점
    }
}