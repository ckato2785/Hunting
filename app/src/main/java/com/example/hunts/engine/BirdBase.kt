package com.example.hunts.model

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.hunts.engine.GameObject
import kotlin.random.Random

/**
 * 모든 새 객체의 기본 구현을 제공하는 추상 클래스입니다.
 * GameObject 인터페이스를 구현합니다.
 */
abstract class BirdBase(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val bitmap: Bitmap,
    private val speed: Float,
    private val startY: Int // 상단 UI 바 높이. 새가 이 높이 아래에서 스폰되도록 합니다.
) : GameObject {

    // GameObject 인터페이스의 필드를 구현합니다.
    override var x: Float = 0f
        set
    override var y: Float = 0f
        set

    /** 새의 충돌 영역 크기 (이미지 너비의 절반) */
    private val hitRadius: Float

    /** 새가 날아갈 방향 (좌: -1, 우: 1) */
    private val direction: Int

    init {
        // 충돌 영역은 비트맵 너비의 절반 정도로 설정하여 터치 영역을 지정합니다.
        hitRadius = bitmap.width / 2f

        // 새가 화면 좌측 또는 우측 중 한 방향에서 랜덤하게 스폰되도록 합니다.
        direction = if (Random.nextBoolean()) 1 else -1

        // 초기 X 위치 설정: 화면 밖에서 시작하여 화면 안으로 진입합니다.
        // direction = 1 (우측 이동) -> x는 -hitRadius (화면 왼쪽 밖)
        // direction = -1 (좌측 이동) -> x는 screenWidth + hitRadius (화면 오른쪽 밖)
        x = if (direction == 1) -hitRadius else screenWidth + hitRadius.toFloat()

        // 초기 Y 위치 설정: 상단 UI 바(startY) 바로 아래부터 화면 상단 경계까지 랜덤 스폰합니다.
        // 50f는 최소한의 마진을 주기 위해 추가했습니다.
        y = Random.nextFloat() * (startY.toFloat() + 50f)
    }

    /**
     * 게임 로직 업데이트: 새를 이동시킵니다.
     */
    override fun update(deltaTime: Float) {
        // X축 이동: 방향과 속도에 따라 이동
        x += direction * speed * deltaTime

        // Y축 이동: 약간 아래로 이동 (화면을 가로질러 나갈 수 있도록)
        y += speed * 0.1f * deltaTime
    }

    /**
     * 캔버스에 새 이미지를 그립니다.
     */
    override fun draw(canvas: Canvas) {
        canvas.save()
        // 새의 방향이 좌측(-1)일 경우 이미지를 좌우 반전합니다.
        if (direction == -1) {
            // x + bitmap.width / 2f, y + bitmap.height / 2f 는 회전의 중심점입니다.
            canvas.scale(-1f, 1f, x + bitmap.width / 2f, y + bitmap.height / 2f)
        }
        canvas.drawBitmap(bitmap, x, y, null)
        canvas.restore()
    }

    /**
     * 터치 좌표가 새의 충돌 영역 내부에 있는지 확인합니다.
     */
    override fun checkHit(touchX: Float, touchY: Float): Boolean {
        val centerX = x + bitmap.width / 2f
        val centerY = y + bitmap.height / 2f

        // 두 점 사이의 거리 제곱 공식: (x2-x1)^2 + (y2-y1)^2
        val distanceSquared = (touchX - centerX) * (touchX - centerX) + (touchY - centerY) * (touchY - centerY)

        // 거리가 hitRadius 제곱보다 작거나 같으면 충돌로 간주합니다.
        return distanceSquared <= hitRadius * hitRadius
    }

    /**
     * 이 함수는 추상 함수이므로 자식 클래스에서 반드시 구현해야 합니다.
     * (점수를 반환하는 역할)
     */
    abstract override fun getScoreValue(): Int
}