package com.example.hunts.engine

import android.graphics.Canvas

//모든 게임 오브젝트(새, 배경 등)가 구현해야 하는 기본 인터페이스

interface GameObject {

    // 객체의 현재 상태를 업데이트합니다.
    // deltaTime: 이전 프레임으로부터 경과된 시간 (밀리초 또는 초)
    fun update(deltaTime: Float)

    // 객체를 화면에 그립니다.
    fun draw(canvas: Canvas)

    // 주어진 좌표(터치)가 객체 영역 안에 있는지 판정합니다.
    fun checkHit(touchX: Float, touchY: Float): Boolean

    // 화면 밖으로 나가 제거해야 할지 여부를 반환합니다.
    fun isOffScreen(): Boolean

    // 이 객체가 획득할 수 있는 점수입니다.
    fun getScoreValue(): Int
}