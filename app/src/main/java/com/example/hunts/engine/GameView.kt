package com.example.hunts.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import com.example.hunts.R // ⭐ 실제 R 클래스 임포트 유지
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

// ⭐ 임시 R 클래스 정의 제거 (실제 안드로이드 R.drawable을 사용합니다)
// class R { ... } // 이 블록은 제거되어야 합니다.


/**
 * 게임 루프를 실행하는 메인 SurfaceView
 */
class GameView(context: Context, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs),
    SurfaceHolder.Callback,
    Runnable
{

    // 1. 변수 정의 및 초기화
    private lateinit var engine: GameEngine
    private var isRunning = false
    private var gameThread: Thread? = null

    private var sparrowBitmap: Bitmap? = null
    private lateinit var defaultBitmap: Bitmap

    // ⭐ 배경 이미지를 위한 변수 추가
    private var backgroundBitmap: Bitmap? = null
    private var currentBackgroundResId: Int = 0 // 현재 로드된 이미지 리소스 ID 추적

    // UI 그리기용 Paint 객체
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
    }

    private val gameOverPaint = Paint().apply {
        color = Color.RED
        textSize = 100f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val buttonTextPaint = Paint().apply { // 버튼 텍스트용 Paint
        color = Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER // 텍스트 중앙 정렬
    }

    // ⭐ 버튼 배경/테두리 그리기용 Paint 추가
    private val buttonBackgroundPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
        alpha = 150 // 반투명 배경
    }
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 200 // 반투명 테두리
    }


    init {
        holder.addCallback(this)

        try {
            // 참새 이미지 로드 (android.R.drawable.btn_star_big_on 사용)
            val tempBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.btn_star_big_on)
            sparrowBitmap = Bitmap.createScaledBitmap(tempBitmap, 100, 100, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (sparrowBitmap == null) {
            defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            defaultBitmap.eraseColor(Color.GRAY)
            sparrowBitmap = defaultBitmap
        }
    }

    // ⭐ 배경 이미지 로드 및 리사이즈 함수 수정 (실제 로직 적용)
    private fun loadBackground(resId: Int) {
        // 유효하지 않은 ID (0)이거나 이미 로드된 ID와 같으면 리로드하지 않습니다.
        if (resId == 0 || currentBackgroundResId == resId) return

        try {
            // ⭐ 실제 리소스 로드 및 리사이징 로직 적용
            val originalBitmap = BitmapFactory.decodeResource(resources, resId)

            // 기존 배경 이미지가 있다면 재활용
            backgroundBitmap?.recycle()

            // 화면 크기에 맞게 비트맵 리사이징
            backgroundBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            originalBitmap.recycle()

            currentBackgroundResId = resId
        } catch (e: Exception) {
            e.printStackTrace()
            // 리소스 로드 실패 시 null로 설정
            backgroundBitmap = null
            currentBackgroundResId = 0
        }
    }

    // =======================================================
    // 1. SurfaceHolder.Callback 구현
    // =======================================================

    override fun surfaceCreated(holder: SurfaceHolder) {
        val finalBitmap = sparrowBitmap ?: defaultBitmap

        // GameEngine 초기화
        engine = GameEngine(width, height, finalBitmap, this.context)

        // ⭐ 엔진 초기화 후 1단계 로드
        engine.loadStage(1)

        // ⭐ 초기 배경 이미지 로드
        loadBackground(engine.currentStageData.backgroundResId)

        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 화면 크기가 변경될 때 배경 이미지도 다시 로드하여 리사이징합니다.
        // width, height가 변경되었으므로 loadBackground를 다시 호출합니다.
        if (currentBackgroundResId != 0) {
            loadBackground(currentBackgroundResId)
        }
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

    // =======================================================
    // 2. Runnable 구현 (게임 루프)
    // =======================================================

    override fun run() {
        var canvas: Canvas? = null
        while (isRunning) {
            try {
                // 1. 게임 로직 업데이트
                engine.update()

                // 2. 화면 잠그고 그리기 준비
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {

                        // ⭐ 3. 배경 이미지 그리기
                        drawBackground(canvas)

                        // 4. 그리기 실행 (엔진의 오브젝트 그리기)
                        engine.draw(canvas)

                        // 5. UI 그리기 (GameView에서 담당)
                        drawUI(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 6. 화면 해제 및 표시
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    // ⭐ 배경을 그리는 함수 (스테이지 배경 ID 검사 및 로드)
    private fun drawBackground(canvas: Canvas) {
        // GameEngine에서 새로운 스테이지가 로드되었는지 확인하고 로드
        if (currentBackgroundResId != engine.currentStageData.backgroundResId) {
            loadBackground(engine.currentStageData.backgroundResId)
        }

        // 배경 이미지 그리기.
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
        } else {
            // 이미지 로드 실패 또는 배경 ID가 0인 경우, 임시 배경 색상 표시
            val bgColor = when (engine.currentStageIndex) {
                1 -> Color.rgb(0, 50, 0) // Stage 1 (Morning)
                2 -> Color.rgb(50, 0, 50) // Stage 2 (Highnoon)
                3 -> Color.rgb(0, 0, 50) // Stage 3 (Evening)
                4 -> Color.rgb(20, 20, 20) // Stage 4 (Dawn/Night)
                else -> Color.BLACK
            }
            canvas.drawColor(bgColor)
        }
    }

    /**
     * 점수판 및 게임 오버 화면을 그리는 함수 (3.1~3.6 기능 구현)
     */
    private fun drawUI(canvas: Canvas) {
        val score = engine.scoreManager.score
        // 3.1 타이머 표시 (Int로 반올림된 시간)
        val timeLeft = engine.scoreManager.timeLeft
        val gameState = engine.gameState

        // 1. 점수 및 남은 시간 표시
        canvas.drawText("SCORE: $score", 50f, 70f, scorePaint)
        canvas.drawText("TIME: ${engine.scoreManager.timeLeftFormatted} s", width - 300f, 70f, scorePaint) // 소수점 한 자리 표시

        // ⭐ 현재 스테이지 번호 표시 추가
        val stageText = "STAGE: ${engine.currentStageIndex} / ${StageManager.totalStages}"
        val boundsStage = Rect()
        scorePaint.getTextBounds(stageText, 0, stageText.length, boundsStage)
        canvas.drawText(stageText, width / 2f - boundsStage.width() / 2f, 70f, scorePaint)


        // 2. 게임 종료 화면 (3.2, 3.3, 3.4, 3.5, 3.6 구현)
        if (gameState == GameState.END) {

            // 3.6 화면 전환 애니메이션 (페이드 효과)
            val fadeProgress = (engine.endScreenTimer / engine.FADE_DURATION).coerceIn(0f, 1f)
            val alpha = (fadeProgress * 180).toInt() // 최대 불투명도 180
            canvas.drawColor(Color.argb(alpha, 0, 0, 0))

            // 페이드 인이 어느 정도 진행된 후 UI 표시
            if (engine.endScreenTimer > 0.5f) {

                val isLastStage = StageManager.isLastStage(engine.currentStageIndex)

                // 2-1. 성공/실패 텍스트 (타이틀) (3.3 기준 점수 달성 텍스트)
                val resultText = when {
                    engine.isStageSuccess && isLastStage -> "최종 승리!"
                    engine.isStageSuccess -> "스테이지 성공!"
                    else -> "실패"
                }

                val resultPaint = if (engine.isStageSuccess) gameOverPaint.apply { color = Color.YELLOW }
                else gameOverPaint.apply { color = Color.RED }

                var bounds = Rect()
                resultPaint.getTextBounds(resultText, 0, resultText.length, bounds)
                val xResult = (width / 2f) - (bounds.width() / 2f)
                val yResult = (height / 2f) - 250f
                canvas.drawText(resultText, xResult, yResult, resultPaint)

                // 2-2. 최종 점수 및 목표 점수 표시
                val textCurrentScore = "최종 점수: $score / 목표: ${engine.scoreManager.targetScore}"
                scorePaint.getTextBounds(textCurrentScore, 0, textCurrentScore.length, bounds)
                val xCurrentScore = (width / 2f) - (bounds.width() / 2f)
                val yCurrentScore = yResult + 100f
                canvas.drawText(textCurrentScore, xCurrentScore, yCurrentScore, scorePaint)

                // 2-3. 최고 점수 표시 (3.4 최고 점수)
                val textHighestScore = "최고 점수: ${engine.highestScore}"
                scorePaint.getTextBounds(textHighestScore, 0, textHighestScore.length, bounds)
                val xHighestScore = (width / 2f) - (bounds.width() / 2f)
                val yHighestScore = yCurrentScore + 70f
                canvas.drawText(textHighestScore, xHighestScore, yHighestScore, scorePaint)

                // 2-4. 버튼 그리기: 다시 시작 / 다음 스테이지 (3.5 버튼 처리)
                val buttonY = height / 2f + 200f
                val buttonWidth = 300
                val buttonHeight = 80
                val buttonMargin = 100

                // 🔴 다시 시작 버튼 (좌측) - 현재 스테이지 재도전
                val xRestartBtn = width / 2f - buttonWidth - buttonMargin / 2f
                val yRestartBtn = buttonY

                val restartButtonBounds = Rect(
                    xRestartBtn.toInt(),
                    (yRestartBtn - buttonHeight / 2f).toInt(),
                    (xRestartBtn + buttonWidth).toInt(),
                    (yRestartBtn + buttonHeight / 2f).toInt()
                )
                val restartButtonRectF = RectF(restartButtonBounds)

                canvas.drawRoundRect(restartButtonRectF, 10f, 10f, buttonBackgroundPaint)
                canvas.drawRoundRect(restartButtonRectF, 10f, 10f, buttonBorderPaint)
                canvas.drawText("재도전", xRestartBtn + buttonWidth / 2f, yRestartBtn + buttonHeight * 0.35f, buttonTextPaint)

                // 🟢 다음 스테이지 버튼 (우측) - 조건에 따라 텍스트 변경 (6.2)
                val nextButtonText = when {
                    engine.isStageSuccess && isLastStage -> "처음으로" // 최종 승리
                    engine.isStageSuccess -> "다음 스테이지" // 스테이지 성공
                    else -> "재도전" // 실패 (재도전 버튼 역할)
                }

                val xNextStageBtn = width / 2f + buttonMargin / 2f
                val yNextStageBtn = buttonY

                val nextStageButtonBounds = Rect(
                    xNextStageBtn.toInt(),
                    (yNextStageBtn - buttonHeight / 2f).toInt(),
                    (xNextStageBtn + buttonWidth).toInt(),
                    (yNextStageBtn + buttonHeight / 2f).toInt()
                )
                val nextStageButtonRectF = RectF(nextStageButtonBounds)

                canvas.drawRoundRect(nextStageButtonRectF, 10f, 10f, buttonBackgroundPaint)
                canvas.drawRoundRect(nextStageButtonRectF, 10f, 10f, buttonBorderPaint)
                canvas.drawText(nextButtonText, xNextStageBtn + buttonWidth / 2f, yNextStageBtn + buttonHeight * 0.35f, buttonTextPaint)

                // 엔진에 버튼 영역 정보 전달 (터치 처리에 사용)
                engine.setButtonBounds(restartButtonBounds, nextStageButtonBounds)
            }
        } else {
            // 게임 RUNNING 상태일 때는 버튼 영역을 초기화
            engine.setButtonBounds(null, null)
        }
    }

    // =======================================================
    // 3. 터치 이벤트 처리
    // =======================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            engine.handleTouch(event.x, event.y)
            return true
        }
        return super.onTouchEvent(event)
    }
}