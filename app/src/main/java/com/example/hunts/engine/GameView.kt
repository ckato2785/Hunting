package com.example.hunts // 실제 패키지명으로 변경해주세요

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.example.hunts.engine.GameEngine
import com.example.hunts.engine.GameState
import com.example.hunts.engine.StageManager
import kotlin.random.Random
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.util.AttributeSet
import androidx.core.graphics.drawable.toBitmap
import com.example.hunts.R // R 클래스 import가 필요합니다.

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    // --- ⭐ [추가/수정] 오류가 나는 변수 선언 ---
    private var currentBackgroundResId: Int = 0 // 현재 로드된 배경 리소스 ID 저장
    private var backgroundBitmap: Bitmap? = null // 현재 로드된 배경 이미지 Bitmap 저장
    // ----------------------------------------

    private var isRunning = false
    private var gameThread: Thread? = null

    private var engine: GameEngine? = null

    // 임시 Sparrow 비트맵 (실제 게임 리소스로 대체해야 함)
    private val sparrowBitmap: Bitmap by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.toBitmap(100, 100)
            ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }
    private val defaultBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 100f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var restartButtonBounds: Rect? = null
    private var nextStageButtonBounds: Rect? = null

    init {
        holder.addCallback(this)
        // 화면 터치 이벤트 리스너 설정
        setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                engine?.handleTouch(event.x, event.y)
                true
            } else {
                false
            }
        }
    }

    // ⭐ [수정] 오류가 나는 loadBackground 함수 로직 수정
    /**
     * 배경 이미지를 로드하고 currentBackgroundResId와 backgroundBitmap을 업데이트합니다.
     */
    private fun loadBackground(resId: Int) {
        if (resId == 0) {
            backgroundBitmap = null
            currentBackgroundResId = 0
            return
        }

        currentBackgroundResId = resId
        try {
            // 리소스 ID로 이미지를 로드하고 화면 크기에 맞게 조정합니다.
            val drawable = ContextCompat.getDrawable(context, resId)
            if (drawable != null && width > 0 && height > 0) {
                backgroundBitmap = drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
            } else {
                backgroundBitmap = null
            }
        } catch (e: Exception) {
            backgroundBitmap = null
            // R.drawable.ic_menu_crop 등 안드로이드 내장 드로어블을 임시로 사용했기 때문에 오류가 날 수 있습니다.
            // 실제 앱에서는 R.drawable.your_image_name 처럼 실제 리소스를 사용해야 합니다.
            e.printStackTrace()
        }
    }

    // ⭐ [수정] 오류가 나는 drawBackground 함수 로직 수정
    /**
     * 배경을 그리는 함수 (배경 리소스 ID 변경 감지 기능 포함)
     */
    private fun drawBackground(canvas: Canvas) {
        val engine = engine ?: return

        // GameEngine에서 새로운 스테이지가 로드되었는지 확인
        if (currentBackgroundResId != engine.currentStageData.backgroundResId) {
            loadBackground(engine.currentStageData.backgroundResId)
        }

        // 배경 이미지 그리기. 이미지가 로드되지 않았다면 검은색으로 채웁니다.
        if (backgroundBitmap != null) {
            // 화면 전체를 덮도록 그립니다.
            canvas.drawBitmap(
                bitmap = backgroundBitmap!!,
                src = Rect(0, 0, backgroundBitmap!!.width, backgroundBitmap!!.height),
                dst = Rect(0, 0, width, height),
                paint = null
            )
        } else {
            canvas.drawColor(Color.BLACK)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val finalBitmap = sparrowBitmap ?: defaultBitmap

        // GameEngine 초기화
        engine = GameEngine(
            screenWidth = width,
            screenHeight = height,
            sparrowBitmap = finalBitmap,
            context = this.context
        )

        // ⭐ [수정] 초기 배경 이미지 로드 (engine 초기화 후)
        engine?.currentStageData?.let {
            loadBackground(it.backgroundResId)
        }

        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 화면 크기가 변경될 때 배경 이미지도 다시 로드하며 리사이징합니다.
        if (currentBackgroundResId != 0) {
            loadBackground(currentBackgroundResId)
        }
        // 버튼 영역도 화면 크기에 맞게 재계산이 필요할 수 있습니다.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    override fun run() {
        while (isRunning) {
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(holder) {
                        engine?.update()
                        drawGame(canvas)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        val engine = engine ?: return
        val gameManager = engine.scoreManager
        val gameState = engine.gameState

        // 1. 배경 그리기
        drawBackground(canvas)

        // 2. 오브젝트 그리기
        engine.draw(canvas)

        // 3. 점수 및 남은 시간 표시
        canvas.drawText("SCORE: ${gameManager.score}", 50f, 70f, scorePaint)
        canvas.drawText("TIME: ${gameManager.timeLeftFormatted} s", width - 50f, 70f, scorePaint.apply { textAlign = Paint.Align.RIGHT })

        // ⭐ [수정] 현재 스테이지 번호 표시 추가 (오류 해결)
        canvas.drawText("STAGE: ${engine.currentStageIndex}", width / 2f, 70f, scorePaint.apply { textAlign = Paint.Align.CENTER })

        // 4. 게임 종료 화면 (3.2초 시점 END 상태)
        if (gameState == GameState.END) {
            // 4.1. 화면 전환 애니메이션 (페이드 효과)
            val fadeProgress = engine.endScreenTimer / engine.FADE_DURATION.coerceIn(0.1f, 1f)
            val alpha = (fadeProgress.coerceIn(0f, 1f) * 180).toInt() // 최대 불투명도 180
            canvas.drawColor(Color.argb(alpha, 0, 0, 0))

            if (engine.endScreenTimer > 0.5f) { // 페이드가 어느 정도 진행된 후 텍스트 표시
                val isLastStage = StageManager.isLastStage(engine.currentStageIndex)
                val isSuccess = engine.isStageSuccess

                // 4.2. 성공/실패 텍스트 (타이틀)
                val resultText = when {
                    isSuccess && isLastStage -> "최종 승리!"
                    isSuccess -> "스테이지 성공!"
                    else -> "실패..."
                }
                canvas.drawText(resultText, width / 2f, height / 2f - 150f, titlePaint)

                // 4.3. 점수 표시
                canvas.drawText("점수: ${gameManager.score} / ${gameManager.targetScore}", width / 2f, height / 2f, scorePaint)
                canvas.drawText("최고 점수: ${engine.highestScore}", width / 2f, height / 2f + 80f, scorePaint)

                // 4.4. 버튼 그리기
                val buttonHeight = 120
                val buttonWidth = 350
                val padding = 50
                val centerY = height / 2f + 250f

                // 버튼 그리기 Paint
                val buttonPaint = Paint().apply {
                    color = Color.parseColor("#4CAF50") // 녹색 배경
                    style = Paint.Style.FILL
                }
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 50f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                // Restart 버튼 영역
                val restartLeft = width / 2 - buttonWidth - padding / 2
                val restartRight = width / 2 - padding / 2
                val restartTop = (centerY - buttonHeight / 2).toInt()
                val restartBottom = (centerY + buttonHeight / 2).toInt()
                restartButtonBounds = Rect(restartLeft, restartTop, restartRight, restartBottom)
                canvas.drawRect(restartButtonBounds!!, buttonPaint.apply { color = Color.RED })
                canvas.drawText("다시 하기", restartButtonBounds!!.centerX().toFloat(), restartButtonBounds!!.centerY().toFloat() + 15f, textPaint)

                // Next Stage / Restart 버튼 영역
                val nextLeft = width / 2 + padding / 2
                val nextRight = width / 2 + buttonWidth + padding / 2
                nextStageButtonBounds = Rect(nextLeft, restartTop, nextRight, restartBottom)

                val nextButtonText = if (isSuccess && !isLastStage) "다음 스테이지" else "재도전"
                val nextButtonColor = if (isSuccess) Color.parseColor("#2196F3") else Color.parseColor("#FF9800") // 파란색 또는 주황색
                canvas.drawRect(nextStageButtonBounds!!, buttonPaint.apply { color = nextButtonColor })
                canvas.drawText(nextButtonText, nextStageButtonBounds!!.centerX().toFloat(), nextStageButtonBounds!!.centerY().toFloat() + 15f, textPaint)


                // GameEngine에 버튼 영역 업데이트
                engine.setButtonBounds(restartButtonBounds, nextStageButtonBounds)
            }
        }
    }
}