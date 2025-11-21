package com.example.hunts.engine

/**
 * 게임의 각 스테이지에 대한 설정 데이터를 저장하는 데이터 클래스입니다.
 * StageManager.kt에서 이 데이터를 사용하여 스테이지를 초기화합니다.
 *
 * @property stageId 현재 스테이지의 고유 번호
 * @property targetScore 해당 스테이지를 완료하기 위해 필요한 목표 점수
 * @property gameDuration 해당 스테이지의 제한 시간 (초 단위)
 * @property spawnInterval 새가 생성되는 간격 (초 단위)
 * @property sparrowSpeed 참새 (가장 빠름)의 이동 속도 (픽셀/초)
 * @property buntingSpeed 멧새 (중간)의 이동 속도 (픽셀/초)
 * @property magpieSpeed 까치 (가장 느림, 고득점)의 이동 속도 (픽셀/초)
 */
data class StageData(
    val stageId: Int,
    val targetScore: Int,
    val gameDuration: Float,
    val spawnInterval: Float,
    val sparrowSpeed: Float,
    val buntingSpeed: Float,
    val magpieSpeed: Float
)