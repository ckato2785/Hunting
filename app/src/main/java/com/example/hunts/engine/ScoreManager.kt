package com.example.hunts.engine

/**
 * 게임의 점수, 시간 등 메타 정보를 관리하는 클래스
 */
class ScoreManager {
    // 1. 점수 변수
    var score: Int = 0
        private set // 외부에서 값을 직접 변경하는 것을 방지

    // 2. 총 게임 시간 설정 (예: 60초)
    private val gameDuration: Float = 60f

    // 3. 현재 경과된 게임 시간
    var gameTime: Float = 0f
        private set

    // 4. 남은 시간 (computed property)
    val timeLeft: Int
        get() = (gameDuration - gameTime).coerceAtLeast(0f).toInt() // 0 미만 방지

    // 5. 점수 추가 메서드 (참새를 맞췄을 때 호출)
    fun addScore(points: Int) {
        score += points
    }

    // 6. 시간 업데이트 메서드
    fun updateTime(deltaTime: Float) {
        // 타이머가 0이 아니면 시간을 계속 누적합니다.
        if (gameTime < gameDuration) {
            gameTime += deltaTime
        }
    }

    // 7. 게임 종료 여부 확인
    fun isTimeUp(): Boolean {
        return gameTime >= gameDuration
    }

    // 8. 게임 상태 재설정
    fun reset() {
        score = 0
        gameTime = 0f
    }
}
