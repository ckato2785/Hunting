package com.example.hunts.engine

import android.graphics.Canvas // Canvas를 draw 메서드에서 사용하기 위해 추가

/**
 * 게임 내 모든 상호작용 가능한 객체(예: 새, 플레이어, 폭탄)의 기본 계약(인터페이스)입니다.
 * 모든 게임 객체는 이 인터페이스를 구현해야 합니다.
 */
interface GameObject {
    // 객체의 x 좌표 (읽기/쓰기)
    var x: Float
    // 객체의 y 좌표 (읽기/쓰기)
    var y: Float
    // 객체의 너비 (읽기 전용)
    val width: Float
    // 객체의 높이 (읽기 전용)
    val height: Float

    /**
     * 게임 로직 업데이트 (예: 이동, 상태 변화)를 처리합니다.
     * @param deltaTime 이전 프레임으로부터 경과된 시간 (초 단위)
     */
    fun update(deltaTime: Float)

    /**
     * 객체를 화면에 그립니다.
     * @param canvas 객체가 그려질 캔버스
     */
    fun draw(canvas: Canvas) // Sparrow에서 사용하도록 Canvas 인자를 추가

    /**
     * 특정 터치 좌표가 객체 영역 내에 있는지 확인합니다 (클릭/터치 처리).
     * @param touchX 터치된 x 좌표
     * @param touchY 터치된 y 좌표
     * @return 객체가 터치되었으면 true
     */
    fun checkHit(touchX: Float, touchY: Float): Boolean // Sparrow에서 사용하도록 추가

    /**
     * 객체가 화면 밖으로 완전히 벗어났는지 확인합니다.
     * @param screenHeight 화면 높이
     * @return 화면 밖으로 벗어났으면 true
     */
    fun isOffScreen(screenHeight: Int): Boolean // Sparrow에서 사용하도록 추가

    /**
     * 객체가 터치되었을 때 얻을 수 있는 점수를 반환합니다.
     */
    fun getScoreValue(): Int // Sparrow에서 사용하도록 추가

    /**
     * 다른 게임 객체와의 충돌 여부를 확인합니다.
     * @param other 충돌을 확인할 다른 GameObject
     * @return 충돌하면 true, 아니면 false
     */
    fun checkCollision(other: GameObject): Boolean {
        // 간단한 사각형 충돌 감지(AABB) 구현
        return (x - width / 2 < other.x + other.width / 2 &&
                x + width / 2 > other.x - other.width / 2 &&
                y - height / 2 < other.y + other.height / 2 &&
                y + height / 2 > other.y - other.height / 2)
    }
}