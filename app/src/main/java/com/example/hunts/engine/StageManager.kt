package com.example.hunts.engine

// 1. R 클래스를 사용하기 위해 Android 프로젝트의 R 파일을 임포트합니다.
import com.example.hunts.R

/**
 * 스테이지의 배경 이미지 ID (실제 프로젝트에서는 R.drawable.xxx 와 같은 리소스 ID가 들어감)
 * 현재는 컴파일을 위해 임시로 Int 값을 사용합니다.
 */
data class StageData(
    val stageIndex: Int,
    val gameDuration: Float,     // 게임 제한 시간 (초)
    val targetScore: Int,        // 스테이지 목표 점수
    val spawnInterval: Float,    // 참새 스폰 간격 (초) - 난이도
    val sparrowSpeed: Float,     // ⭐ 참새의 이동 속도 (픽셀/초) - 난이도
    val backgroundResId: Int     // 배경 리소스 ID
)

/**
 * 스테이지 데이터를 관리하는 싱글턴 객체
 */
object StageManager {

    // 2. 임시 리소스 ID 대신, 실제 R.drawable.xxx 리소스 ID를 사용합니다.
    // 임시 상수 (BG_MORNING, BG_DAY 등)는 이제 필요하지 않습니다.

    // 6.1 StageData 배열로 각 스테이지 구성
    private val stages = listOf(
        StageData(
            stageIndex = 1,
            gameDuration = 60f,
            targetScore = 150,
            spawnInterval = 1.0f,
            sparrowSpeed = 500f,
            backgroundResId = R.drawable.morning
        ),
        StageData(
            stageIndex = 2,
            gameDuration = 60f,
            targetScore = 300,
            spawnInterval = 0.8f, // 스폰 주기 단축 (난이도 상승)
            sparrowSpeed = 650f,
            backgroundResId = R.drawable.highnoon
        ),
        StageData(
            stageIndex = 3,
            gameDuration = 60f,
            targetScore = 500,
            spawnInterval = 0.6f,
            sparrowSpeed = 800f,
            backgroundResId = R.drawable.evening
        ),
        StageData(
            stageIndex = 4,
            gameDuration = 60f,
            targetScore = 750,
            spawnInterval = 0.5f,
            sparrowSpeed = 1000f,
            backgroundResId = R.drawable.dawn
        )
    )

    /**
     * 특정 인덱스의 스테이지 데이터를 반환합니다. (스테이지 인덱스는 1부터 시작)
     * 인덱스가 범위를 벗어나면 첫 스테이지 데이터(1)를 반환합니다.
     */
    fun getStageData(stageIndex: Int): StageData {
        // 스테이지 인덱스는 1부터 시작하므로 0-based 인덱스로 변환 (stageIndex - 1)
        return stages.getOrElse(stageIndex - 1) { stages.first() }
    }

    /**
     * 현재 스테이지의 다음 스테이지 데이터를 반환합니다.
     * 다음 스테이지가 없으면 (마지막 스테이지인 경우) null을 반환합니다.
     */
    fun getNextStageData(currentStageIndex: Int): StageData? {
        val nextIndex = currentStageIndex
        // stages 리스트는 0부터 시작하므로, 다음 스테이지 데이터는 nextIndex에 위치함
        // 예: 현재 1스테이지 (인덱스 0) -> 다음 2스테이지 (인덱스 1)
        return stages.getOrNull(nextIndex)
    }

    /**
     * 다음 스테이지가 있는지 확인합니다.
     */
    fun isLastStage(currentStageIndex: Int): Boolean {
        return currentStageIndex >= stages.size
    }

    /**
     * 스테이지 개수
     */
    val totalStages: Int
        get() = stages.size
}