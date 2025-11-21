package com.example.hunts.engine

/**
 * 모든 스테이지 데이터를 관리하고 스테이지 진행 로직을 제공하는 싱글톤 객체입니다.
 */
object StageManager {
    // StageData 정의는 이전 응답에서 제공된 StageData.kt 파일에 의존합니다.
    private val stages = mapOf(
        1 to StageData(
            stageId = 1,
            targetScore = 100,
            gameDuration = 30f,
            spawnInterval = 1.5f,
            sparrowSpeed = 200f,
            buntingSpeed = 150f,
            magpieSpeed = 100f
        ),
        2 to StageData(
            stageId = 2,
            targetScore = 250,
            gameDuration = 45f,
            spawnInterval = 1.0f,
            sparrowSpeed = 250f,
            buntingSpeed = 200f,
            magpieSpeed = 150f
        ),
        3 to StageData(
            stageId = 3,
            targetScore = 500,
            gameDuration = 60f,
            spawnInterval = 0.7f,
            sparrowSpeed = 300f,
            buntingSpeed = 250f,
            magpieSpeed = 200f
        )
        // 여기에 더 많은 스테이지를 추가할 수 있습니다.
    )

    /**
     * 특정 스테이지 번호의 데이터를 가져옵니다.
     * 맵에 없으면 안전하게 1단계 데이터를 반환합니다.
     */
    fun getStageData(stageIndex: Int): StageData {
        return stages[stageIndex] ?: stages.getValue(1)
    }

    /**
     * [오류 수정] 다음 스테이지(currentStageIndex + 1)가 StageMap에 존재하는지 확인합니다.
     * StageManager.hasNextStage(currentStageIndex) 호출 오류를 해결합니다.
     */
    fun hasNextStage(currentStageIndex: Int): Boolean {
        return stages.containsKey(currentStageIndex + 1)
    }

    /**
     * 총 스테이지 개수를 반환합니다.
     */
    fun getTotalStages(): Int {
        return stages.size
    }
}