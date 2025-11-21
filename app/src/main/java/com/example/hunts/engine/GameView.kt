package com.example.hunts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface // Typeface 임포트 추가
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat
import com.example.hunts.engine.GameEngine
import com.example.hunts.engine.GameState
import kotlin.random.Random

/**
 * 게임의 캔버스 및 그리기 스레드를 관리합니다.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val gameEngine: GameEngine // GameEngine은 이미 로드된 비트맵을 받습니다.
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var thread: GameThread? = null
    private var isSurfaceCreated = false

    // Paint 객체 (UI 그리기용)
    private val uiPaint = Paint().apply { textSize = 50f }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        textAlign = Paint.Align.CENTER
        // ⭐ 수정: R.font.inter_bold 대신 안전한 시스템 기본 굵은 글씨체 사용
        typeface = Typeface.DEFAULT_BOLD
    }
    private val buttonPaint = Paint()

    // UI 영역 정의
    private var topUiBarBounds: Rect = Rect()
    private var bottomUiBarBounds: Rect = Rect()

    // 버튼 영역 정의
    private var prevStageButtonBounds: Rect? = null
    private var nextStageButtonBounds: Rect? = null
    private var pauseButtonBounds: Rect? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        // 이미지 로딩 로직은 MainActivity로 이동합니다.
    }

    // =======================================================
    // 1. SurfaceView 콜백 (뷰 생성/변경/소멸)
    // =======================================================

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isSurfaceCreated) {
            // UI 바 영역 계산
            val topBarHeight = height / 10
            val bottomBarHeight = height / 10

            topUiBarBounds = Rect(0, 0, width, topBarHeight)
            bottomUiBarBounds = Rect(0, height - bottomBarHeight, width, height)

            // GameEngine에 상단 UI 높이 전달 (스폰 Y 좌표 조정)
            gameEngine.setTopUiBarHeight(topBarHeight)

            thread = GameThread(holder)
            thread?.setRunning(true)
            thread?.start()
            isSurfaceCreated = true
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread?.setRunning(false)
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        isSurfaceCreated = false
    }

    // =======================================================
    // 2. 게임 루프 (그리기)
    // =======================================================

    /**
     * 게임 화면을 그리는 메인 함수입니다.
     */
    private fun drawGame(canvas: Canvas) {
        // 배경 그리기
        canvas.drawColor(Color.rgb(135, 206, 235)) // 하늘색

        // GameEngine의 오브젝트 그리기 (새, 파티클 등)
        gameEngine.draw(canvas)

        // UI 바 그리기 (항상 맨 위에 그려집니다)
        drawUI(canvas)

        // 상태별 오버레이 그리기 (READY, PAUSED, END)
        when (gameEngine.gameState) {
            GameState.READY -> drawReadyUI(canvas)
            GameState.PAUSED -> drawPauseUI(canvas)
            GameState.END -> drawEndGameUI(canvas)
            GameState.RUNNING -> { /* 아무것도 그리지 않음 */ }
        }
    }

    // =======================================================
    // 3. UI 그리기 함수
    // =======================================================

    /**
     * 상단 및 하단 UI 바를 그립니다.
     */
    private fun drawUI(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        // 1. 상단 바 (SCORE/TIME/TARGET)
        uiPaint.color = Color.rgb(30, 144, 255) // 파란색
        canvas.drawRect(topUiBarBounds, uiPaint)

        val timeFormatted = String.format("%.1f", gameEngine.scoreManager.remainingTime)

        textPaint.textSize = 36f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("TARGET: ${gameEngine.currentStageData.targetScore}", 20f, topUiBarBounds.centerY() + 15f, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("TIME: ${timeFormatted}s", w / 2, topUiBarBounds.centerY() + 15f, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("SCORE: ${gameEngine.scoreManager.score}", w - 20f, topUiBarBounds.centerY() + 15f, textPaint)

        // 2. 하단 바 (STAGE/PAUSE)
        uiPaint.color = Color.rgb(0, 128, 0) // 녹색
        canvas.drawRect(bottomUiBarBounds, uiPaint)

        val buttonW = bottomUiBarBounds.width() / 5
        val buttonH = bottomUiBarBounds.height()
        val btnY = bottomUiBarBounds.top

        // STAGE 텍스트
        textPaint.textSize = 50f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("STAGE ${gameEngine.currentStageIndex}", 20f, btnY + buttonH / 2 + 15f, textPaint)

        // PAUSE 버튼 (오른쪽 하단)
        pauseButtonBounds = Rect(w.toInt() - buttonW, btnY, w.toInt(), btnY + buttonH)
        buttonPaint.color = Color.rgb(255, 140, 0) // 주황색
        canvas.drawRect(pauseButtonBounds!!, buttonPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.BLACK
        canvas.drawText("PAUSE", pauseButtonBounds!!.centerX().toFloat(), pauseButtonBounds!!.centerY().toFloat() + 15f, textPaint)
        textPaint.color = Color.WHITE // 기본 색상 복원

        // GameEngine에 버튼 영역 전달
        // RUNNING 상태일 때만 PAUSE 버튼 활성화
        if (gameEngine.gameState == GameState.RUNNING) {
            gameEngine.setStageButtonBounds(prevBtn = null, nextBtn = null, pauseBtn = pauseButtonBounds)
        } else {
            // 다른 상태에서는 PAUSE 버튼 비활성화
            gameEngine.setStageButtonBounds(prevBtn = null, nextBtn = null, pauseBtn = null)
        }
    }

    /**
     * READY (시작 전) 오버레이를 그립니다.
     */
    private fun drawReadyUI(canvas: Canvas) {
        canvas.drawColor(Color.argb(180, 0, 0, 0)) // 반투명 검은색 오버레이
        textPaint.textSize = 100f
        textPaint.color = Color.WHITE
        canvas.drawText("TAP TO START", canvas.width / 2f, canvas.height / 2f, textPaint)

        // 전체 화면을 시작 버튼으로 사용
        nextStageButtonBounds = Rect(0, 0, canvas.width, canvas.height)
        gameEngine.setStageButtonBounds(prevBtn = null, nextBtn = nextStageButtonBounds, pauseBtn = null)
    }

    /**
     * PAUSED (일시정지) 오버레이를 그립니다.
     */
    private fun drawPauseUI(canvas: Canvas) {
        canvas.drawColor(Color.argb(180, 0, 0, 0)) // 반투명 검은색 오버레이
        textPaint.textSize = 120f
        textPaint.color = Color.WHITE
        canvas.drawText("PAUSED", canvas.width / 2f, canvas.height / 3f, textPaint)

        // RESUME 버튼 (가운데)
        val buttonWidth = canvas.width / 3
        val buttonHeight = 150
        val btnX = (canvas.width - buttonWidth) / 2
        val btnY = canvas.height / 2

        prevStageButtonBounds = Rect(btnX, btnY, btnX + buttonWidth, btnY + buttonHeight)
        buttonPaint.color = Color.rgb(255, 140, 0) // 주황색
        canvas.drawRect(prevStageButtonBounds!!, buttonPaint)
        textPaint.textSize = 70f
        textPaint.color = Color.BLACK
        canvas.drawText("RESUME", prevStageButtonBounds!!.centerX().toFloat(), prevStageButtonBounds!!.centerY().toFloat() + 25f, textPaint)

        // GameEngine에 버튼 영역 전달
        gameEngine.setStageButtonBounds(prevBtn = prevStageButtonBounds, nextBtn = null, pauseBtn = null)
    }

    /**
     * END (게임 종료) 오버레이를 그립니다.
     */
    private fun drawEndGameUI(canvas: Canvas) {
        val isSuccess = gameEngine.isStageSuccess
        val targetScore = gameEngine.currentStageData.targetScore
        val finalScore = gameEngine.scoreManager.score

        // 반투명 배경
        canvas.drawColor(Color.argb(220, 0, 0, 0))

        // 결과 상자
        val boxW = canvas.width * 0.8f
        val boxH = canvas.height * 0.45f
        val boxX = (canvas.width - boxW) / 2f
        val boxY = (canvas.height - boxH) / 2f
        val boxRect = Rect(boxX.toInt(), boxY.toInt(), (boxX + boxW).toInt(), (boxY + boxH).toInt())

        buttonPaint.color = if (isSuccess) Color.rgb(60, 179, 113) else Color.rgb(255, 69, 0) // 성공: 녹색, 실패: 빨간색
        canvas.drawRoundRect(boxRect.left.toFloat(), boxRect.top.toFloat(), boxRect.right.toFloat(), boxRect.bottom.toFloat(), 40f, 40f, buttonPaint)

        // 텍스트
        textPaint.textSize = 100f
        textPaint.color = Color.WHITE
        val title = if (isSuccess) "STAGE CLEARED!" else "STAGE FAILED"
        canvas.drawText(title, canvas.width / 2f, boxY + 120f, textPaint)

        textPaint.textSize = 60f
        canvas.drawText("SCORE: $finalScore (Need $targetScore)", canvas.width / 2f, boxY + 220f, textPaint)

        // 버튼 영역
        val btnW = boxW * 0.4f
        val btnH = 120
        val btnMargin = (boxW - (btnW * 2)) / 3

        val btnY = boxY + boxH - btnH - 60

        // RETRY 버튼 (왼쪽)
        val retryX = boxX + btnMargin
        prevStageButtonBounds = Rect(retryX.toInt(), btnY.toInt(), (retryX + btnW).toInt(), (btnY + btnH).toInt())
        buttonPaint.color = Color.rgb(255, 140, 0)
        canvas.drawRect(prevStageButtonBounds!!, buttonPaint)
        textPaint.textSize = 70f
        textPaint.color = Color.BLACK
        canvas.drawText("RETRY", prevStageButtonBounds!!.centerX().toFloat(), prevStageButtonBounds!!.centerY().toFloat() + 25f, textPaint)

        // NEXT/RETRY 버튼 (오른쪽)
        val nextX = boxX + btnMargin * 2 + btnW
        nextStageButtonBounds = Rect(nextX.toInt(), btnY.toInt(), (nextX + btnW).toInt(), (btnY + btnH).toInt())
        buttonPaint.color = if (isSuccess) Color.rgb(255, 140, 0) else Color.rgb(255, 140, 0)
        canvas.drawRect(nextStageButtonBounds!!, buttonPaint)
        textPaint.textSize = 70f
        textPaint.color = Color.BLACK
        val nextText = if (isSuccess) "NEXT" else "RETRY"
        canvas.drawText(nextText, nextStageButtonBounds!!.centerX().toFloat(), nextStageButtonBounds!!.centerY().toFloat() + 25f, textPaint)

        // GameEngine에 버튼 영역 전달
        gameEngine.setStageButtonBounds(prevBtn = prevStageButtonBounds, nextBtn = nextStageButtonBounds)

        // 딜레이 후 버튼 비활성화 (버튼 깜빡임 방지)
        if (gameEngine.endScreenTimer < 0.5f) {
            gameEngine.setStageButtonBounds(prevBtn = null, nextBtn = null)
        }
    }


    // =======================================================
    // 4. 터치 입력
    // =======================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            gameEngine.handleTouch(event.x, event.y)
            return true
        }
        return false
    }

    // =======================================================
    // 5. 스레드 관리
    // =======================================================

    fun pause() {
        var retry = true
        thread?.setRunning(false)
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun resume() {
        if (isSurfaceCreated) {
            thread = GameThread(holder)
            thread?.setRunning(true)
            thread?.start()
        }
    }


    /**
     * 메인 게임 루프 스레드
     */
    inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        private var running = false

        fun setRunning(isRunning: Boolean) {
            running = isRunning
        }

        override fun run() {
            while (running) {
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    synchronized(surfaceHolder) {
                        if (running) {
                            gameEngine.update()
                            drawGame(canvas!!)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}