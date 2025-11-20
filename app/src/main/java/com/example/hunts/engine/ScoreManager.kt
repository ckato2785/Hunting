package com.example.hunts.engine

/**
 * 게임의 점수, 시간 등 메타 정보를 관리하는 클래스
 */
class ScoreManager {
    var score: Int = 0
        private set

    // ⭐ 목표 점수를 var로 변경하여 GameEngine에서 스테이지 로드 시 설정할 수 있도록 합니다.
    var targetScore: Int = 100

    // 2. 총 게임 시간 설정 (60초로 통일)
    var gameDuration: Float = 60f
        private set

    // 3. 현재 경과된 게임 시간 (ScoreManager는 경과 시간을 추적)
    var gameTime: Float = 0f
        private set

    // ⭐ 남은 시간 (Float으로 반환)
    val timeLeftInSeconds: Float
        get() = (gameDuration - gameTime).coerceAtLeast(0f)

    // ⭐ GameView에서 소수점 한 자리 표시를 위해 사용
    val timeLeftFormatted: String
        get() = String.format("%.1f", timeLeftInSeconds)

    // 4. 남은 시간 (Int 형으로 반환 - GameView에서 사용하던 속성)
    val timeLeft: Int
        get() = timeLeftInSeconds.toInt()

    // 5. 점수 추가 메서드
    fun addScore(points: Int) {
        score += points
    }

    // 6. 시간 업데이트 메서드
    fun updateTime(deltaTime: Float) {
        if (gameTime < gameDuration) {
            gameTime += deltaTime
        }
    }

    // 7. 게임 종료 여부 확인
    fun isTimeUp(): Boolean {
        return gameTime >= gameDuration
    }

    // ⭐ 8. 스테이지 데이터에 따라 초기화 (GameEngine에서 호출)
    fun setStageData(duration: Float, target: Int) {
        gameDuration = duration
        targetScore = target
        reset()
    }

    // 9. 게임 상태 재설정
    fun reset() {
        score = 0
        gameTime = 0f
    }
}