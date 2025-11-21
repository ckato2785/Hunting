package com.example.hunts

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hunts.engine.GameEngine

class MainActivity : ComponentActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 전체 화면 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            )
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // ⭐ 2. GameView가 이미지를 로드할 때까지 사용할 임시 (투명) 플레이스홀더 비트맵을 생성합니다.
        // GameView가 생성되면 곧바로 실제 이미지를 비동기로 로드할 것입니다.
        val transparentPlaceholder = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        // GameEngine이 사용하는 ID와 비트맵을 연결합니다.
        // 처음에는 투명한 비트맵을 전달하여 실제 로딩이 시작될 때까지 기다립니다.
        val birdBitmaps: Map<Int, Bitmap> = mapOf(
            1 to transparentPlaceholder,
            2 to transparentPlaceholder,
            3 to transparentPlaceholder
        )

        val gameEngine = GameEngine(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            birdBitmaps = birdBitmaps
        )

        // 3. GameView 인스턴스 생성 및 GameEngine 전달
        gameView = GameView(context = this, gameEngine = gameEngine)

        // 4. GameView를 액티비티의 전체 콘텐츠 뷰로 설정
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}

private fun GameView.pause() {
    TODO("Not yet implemented")
}

private fun GameView.resume() {
    TODO("Not yet implemented")
}
