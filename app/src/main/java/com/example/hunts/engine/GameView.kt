package com.example.hunts.engine

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 게임 루프를 실행하는 메인 SurfaceView
 */
class GameView(context: Context, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs),
    SurfaceHolder.Callback, // SurfaceView의 상태 변화 감지 (생성/변경/파괴)
    Runnable // 게임 루프를 위한 스레드 구현
{

    private val engine = GameEngine(
        screenWidth = TODO(),
        screenHeight = TODO(),
        sparrowBitmap = TODO()
    ) // ① 생성된 게임 엔진 인스턴스
    private var isRunning = false     // ② 스레드 실행 상태 플래그
    private var gameThread: Thread? = null // ③ 게임 루프 스레드

    init {
        // SurfaceView의 상태 변화 콜백을 자신(this)에게 등록
        holder.addCallback(this)
    }

    // =======================================================
    // 1. SurfaceHolder.Callback 구현
    // =======================================================

    override fun surfaceCreated(holder: SurfaceHolder) {
        // 서피스(화면)가 생성되었을 때 게임 루프 시작
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 화면 크기 등이 변경될 때 (보통 무시)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // 서피스가 파괴될 때 (앱 종료 또는 전환 시) 게임 루프 정지
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join() // 스레드가 완전히 종료될 때까지 대기
                retry = false
            } catch (e: InterruptedException) {
                // 스레드 종료가 방해받으면 재시도
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
                // 1. 게임 로직 업데이트 (이동, 충돌 판정, 스폰 등)
                engine.update()

                // 2. 화면 잠그고 그리기 준비
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        // 3. 그리기 실행
                        engine.draw(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 4. 화면 해제 및 표시
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    // =======================================================
    // 3. 터치 이벤트 처리 (계획 2.1)
    // =======================================================

    // override fun onTouchEvent(event: MotionEvent?): Boolean {
    //    // if (event?.action == MotionEvent.ACTION_DOWN) {
    //    //     engine.handleTouch(event.x, event.y)
    //    //     return true
    //    // }
    //    // return false
    // }
}