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
        // 'this'는 Context 역할을 합니다. GameView는 context만 받는 생성자를 사용합니다.
        gameView = GameView(context = this)

        // 2. GameView를 액티비티의 전체 콘텐츠 뷰로 설정
        // 이 한 줄이 기존의 모든 Compose 코드를 대체하며, GameView가 화면을 차지하게 됩니다.
        setContentView(gameView)

        // 참고: enableEdgeToEdge() 등 Compose 관련 함수는 모두 제거했습니다.
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