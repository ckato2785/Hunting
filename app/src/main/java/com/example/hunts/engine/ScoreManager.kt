package com.example.hunts.engine

/**
 * 게임의 점수와 시간을 관리하는 클래스입니다.
 */
class ScoreManager {
    var score: Int = 0
    var remainingTime: Float = 0f
    private var maxTime: Float = 0f

    /**
     * 남은 시간을 소수점 첫째 자리까지 포맷한 문자열로 반환합니다. (예: 29.5)
     */
    val timeLeftFormatted: String
        get() = String.format("%.1f", remainingTime)

    /**
     * 현재 스테이지의 데이터로 매니저를 초기화합니다.
     */
    fun setStageData(duration: Float, targetScore: Int) {
        maxTime = duration
        remainingTime = duration
    }

    /**
     * 점수를 추가하거나 차감합니다.
     */
    fun addScore(value: Int) {
        score += value
    }

    /**
     * 시간을 업데이트합니다.
     */
    fun updateTime(deltaTime: Float) {
        if (remainingTime > 0) {
            remainingTime -= deltaTime
            if (remainingTime < 0) {
                remainingTime = 0f
            }
        }
    }

    /**
     * 시간이 모두 소진되었는지 확인합니다.
     */
    fun isTimeUp(): Boolean {
        return remainingTime <= 0f
    }

    /**
     * 점수와 시간을 초기 상태로 재설정합니다.
     */
    fun reset(gameDuration: Float) {
        score = 0
        remainingTime = maxTime
    }
}