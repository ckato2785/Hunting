package com.example.hunts

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.hunts.engine.GameView

// Compose 관련 import들은 모두 제거합니다.

class MainActivity : ComponentActivity() {

    // 게임 뷰 인스턴스를 저장할 변수를 선언합니다.
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. GameView 인스턴스 생성
        gameView = GameView(context = this)

        // 2. GameView를 액티비티의 전체 콘텐츠 뷰로 설정
        setContentView(gameView)
    }

    // Activity 라이프사이클 관리를 위해 onPause와 onResume 추가 (선택 사항이지만 중요)
    override fun onResume() {
        super.onResume()
        // GameView 내부에 resumeGame()이 있다면 호출
    }

    override fun onPause() {
        super.onPause()
        // GameView 내부에 pauseGame()이 있다면 호출
    }
}