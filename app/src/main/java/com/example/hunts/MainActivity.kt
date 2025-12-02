package com.example.hunts

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hunts.ui.theme.HuntsTheme
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.abs


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuntsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BirdGameScreen()
                }
            }
        }
    }
}

/**
 * 🐦 새 종류 정의 및 게임 파라미터 설정
 * drawable 파일에 ckato.png(참새), aptto.png(멧새), magpie.png(까치)가 있어야 합니다.
 */
enum class BirdType(
    val description: String,
    val score: Int,
    val drawableId: Int,
    val baseSizeDp: Dp = 60.dp, // 기본 크기 (참새/멧새 기준)
    val sizeFactor: Float, // 크기 배율
    val maxCount: Int // 화면 최대 스폰 개수
) {
    SPARROW( // 참새: 주요 목표 (+5점), 4마리 스폰, 기본 크기
        description = "참새 (+5점)",
        score = 5,
        drawableId = R.drawable.ckato,
        sizeFactor = 1.0f,
        maxCount = 4
    ),
    BUNTING( // 멧새: 감점 (-1점), 2마리 스폰, 기본 크기
        description = "멧새 (-1점)",
        score = -1,
        drawableId = R.drawable.aptto,
        sizeFactor = 1.0f,
        maxCount = 2
    ),
    MAGPIE( // 까치: 감점 (-3점), 3마리 스폰, 1.4배 큰 크기
        description = "까치 (-3점)",
        score = -3,
        drawableId = R.drawable.magpie,
        sizeFactor = 1.4f, // 1.3 ~ 1.5배 사이로 설정
        maxCount = 3
    );

    // 실제 화면에 표시될 Dp 크기를 계산
    val actualSizeDp: Dp
        get() = baseSizeDp * sizeFactor
}

/**
 * 🐦 Bird 데이터 클래스: 화면 상의 개체 정보를 담습니다.
 */
data class Bird(
    val id: Int,
    var position: Offset, // 새의 중심 위치 (Dp)
    val type: BirdType,
    val sizeDp: Dp, // 실제 Dp 크기
    val creationTime: Long = System.currentTimeMillis(),
    val velocityX: Float = 0f,
    val velocityY: Float = 0f
)

/**
 * 게임 상태 클래스
 */
class GameState(
    initialBirds: List<Bird> = emptyList()
) {
    var birds by mutableStateOf(initialBirds)
    var score by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)
    var isGameClear by mutableStateOf(false)
    var timeLeft by mutableStateOf(60)
}

const val CLEAR_SCORE = 100 // <-- 이 부분을 20에서 100으로 변경했습니다.
const val MAX_TOTAL_BIRDS = 9 // 참새(4) + 멧새(2) + 까치(3) = 9

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BirdGameScreen() {
    val gameState = remember { GameState() }
    var showClearDialog by remember { mutableStateOf(false) }

    // 타이머 및 게임 상태 업데이트 로직
    LaunchedEffect(gameState.isGameOver, gameState.isGameClear) {
        if (!gameState.isGameOver && !gameState.isGameClear && gameState.timeLeft > 0) {
            while (true) {
                delay(1000L)
                gameState.timeLeft--

                // 클리어 조건 확인
                if (gameState.score >= CLEAR_SCORE) {
                    gameState.isGameClear = true
                    showClearDialog = true
                    break
                }

                // 타임 아웃 조건 확인
                if (gameState.timeLeft == 0) {
                    gameState.isGameOver = true
                    break
                }

                // 3초가 지난 새 제거
                val currentTime = System.currentTimeMillis()
                gameState.birds = gameState.birds.filter {
                    currentTime - it.creationTime < 3000
                }
            }
        }
    }

    // 🌄 배경 이미지 컨테이너
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            // highnoon.png 파일이 drawable 폴더에 있어야 합니다.
            painter = painterResource(id = R.drawable.highnoon),
            contentDescription = "Game Background: High Noon",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            GameStatusRow(score = gameState.score, timeLeft = gameState.timeLeft)

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val canvasWidthPx = with(density) { maxWidth.toPx() }
                val canvasHeightPx = with(density) { maxHeight.toPx() }

                // 🐦 새 스폰 및 물리 엔진
                LaunchedEffect(key1 = gameState.isGameOver, key2 = gameState.isGameClear) {
                    if (!gameState.isGameOver && !gameState.isGameClear) {
                        while (true) {
                            delay(16) // 약 60 FPS
                            val currentBirds = gameState.birds

                            // 새 스폰 로직
                            if (currentBirds.size < MAX_TOTAL_BIRDS && Random.nextFloat() < 0.1f) {
                                // 현재 최대 스폰 가능 마리 수에 도달하지 않은 새 종류 필터링
                                val availableTypes = BirdType.entries.filter { type ->
                                    currentBirds.count { it.type == type } < type.maxCount
                                }

                                if (availableTypes.isNotEmpty()) {
                                    val typeToSpawn = availableTypes.random()
                                    val newBird = makeNewBird(maxWidth, maxHeight, typeToSpawn)
                                    gameState.birds = currentBirds + newBird
                                }
                            }

                            // 물리 엔진 로직 (새 이동)
                            gameState.birds = updateBirdPositions(
                                gameState.birds,
                                canvasWidthPx,
                                canvasHeightPx,
                                density
                            )
                        }
                    }
                }

                // 각 새를 화면에 그림
                gameState.birds.forEach { bird ->
                    BirdComposable(bird = bird) {
                        // 클릭 시 점수 업데이트 및 새 제거
                        gameState.score += bird.type.score
                        gameState.birds =
                            gameState.birds.filterNot { it.id == bird.id }
                    }
                }
            }
        }

        // 게임 클리어 다이얼로그 표시
        if (showClearDialog) {
            GameClearDialog(
                score = gameState.score,
                onRestart = {
                    showClearDialog = false
                    restartGame(gameState)
                },
                onExit = { /* 실제 앱에서는 Activity 종료 등을 사용 */ }
            )
        }

        // 게임 오버 다이얼로그 표시 (클리어 상태가 아닐 때만)
        if (gameState.isGameOver && !gameState.isGameClear) {
            GameOverDialog(
                score = gameState.score,
                onRestart = { restartGame(gameState) },
                onExit = { /* 실제 앱에서는 Activity 종료 등을 사용 */ }
            )
        }
    }
}

/**
 * 🐦 Bird Composable: 새 이미지를 화면에 그립니다.
 */
@Composable
fun BirdComposable(bird: Bird, onClick: () -> Unit) {
    val birdSizeDp = bird.sizeDp

    Image(
        painter = painterResource(id = bird.type.drawableId),
        contentDescription = "Bird: ${bird.type.description}",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(birdSizeDp)
            // position은 새의 중심 위치입니다. Image 컴포넌트의 (0,0)은 왼쪽 상단이므로,
            // 중심을 맞추기 위해 크기의 절반만큼 offset을 조정합니다.
            .offset(
                x = bird.position.x.dp - birdSizeDp / 2,
                y = bird.position.y.dp - birdSizeDp / 2
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

/**
 * 게임 클리어 다이얼로그
 */
@Composable
fun GameClearDialog(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("🎉 GAME CLEAR! 🎉") },
        text = { Text("축하합니다! $score 점으로 게임을 클리어했습니다.") },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("다시 시작")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("종료")
            }
        }
    )
}

/**
 * 게임 오버 다이얼로그
 */
@Composable
fun GameOverDialog(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("게임 오버") },
        text = { Text("당신의 점수는 $score 점입니다.") },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("다시 시작")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("종료")
            }
        }
    )
}

/**
 * 점수 및 시간 표시 로우
 */
@Composable
fun GameStatusRow(score: Int, timeLeft: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 배경 이미지 때문에 텍스트 색상을 대비가 잘 되도록 설정
        Text(text = "Score: $score / $CLEAR_SCORE", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Time: ${timeLeft}s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * 🐦 새 생성 함수
 */
fun makeNewBird(maxWidth: Dp, maxHeight: Dp, birdType: BirdType): Bird {
    val sizeDp = birdType.actualSizeDp
    val radiusDp = sizeDp / 2

    // 새의 중심 위치를 계산 (경계 밖으로 나가지 않도록)
    val centerX = Random.nextFloat() * (maxWidth.value - 2 * radiusDp.value) + radiusDp.value
    val centerY = Random.nextFloat() * (maxHeight.value - 2 * radiusDp.value) + radiusDp.value

    return Bird(
        id = Random.nextInt(),
        position = Offset(
            x = centerX,
            y = centerY
        ),
        sizeDp = sizeDp,
        type = birdType,
        // 최소 속도 1, 랜덤 방향
        velocityX = (Random.nextFloat() * 2 + 1) * if (Random.nextBoolean()) 1f else -1f,
        velocityY = (Random.nextFloat() * 2 + 1) * if (Random.nextBoolean()) 1f else -1f
    )
}

/**
 * 게임 재시작 함수
 */
fun restartGame(gameState: GameState) {
    gameState.score = 0
    gameState.timeLeft = 60
    gameState.isGameOver = false
    gameState.isGameClear = false
    gameState.birds = emptyList()
}


/**
 * 🐦 새 위치 업데이트 함수: 새를 이동시키고 벽 충돌을 처리합니다.
 */
fun updateBirdPositions(
    birds: List<Bird>,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    density: Density
): List<Bird> {
    return birds.map { bird ->
        with(density) {
            // 새의 반지름 (Dp)
            val radiusDp = bird.sizeDp / 2
            // 반지름을 PX로 변환
            val radiusPx = radiusDp.toPx()

            // 현재 위치 (Dp -> Px 변환)
            var xPx = bird.position.x.dp.toPx()
            var yPx = bird.position.y.dp.toPx()

            // 속도 (Dp/tick -> Px/tick 변환)
            // Note: Compose의 Dp.toPx()는 픽셀 값을 반환하므로, 속도는 단순 Dp 값으로 처리
            val vxPx = bird.velocityX.dp.toPx()
            val vyPx = bird.velocityY.dp.toPx()

            // 위치 업데이트
            xPx += vxPx
            yPx += vyPx

            var newVx = bird.velocityX
            var newVy = bird.velocityY

            // 벽 충돌 감지 및 반전
            // X 축 경계
            if (xPx < radiusPx) {
                newVx = abs(newVx)
            } else if (xPx > canvasWidthPx - radiusPx) {
                newVx = -abs(newVx)
            }
            // Y 축 경계
            if (yPx < radiusPx) {
                newVy = abs(newVy)
            } else if (yPx > canvasHeightPx - radiusPx) {
                newVy = -abs(newVy)
            }

            // 경계 이탈 방지
            xPx = xPx.coerceIn(radiusPx, canvasWidthPx - radiusPx)
            yPx = yPx.coerceIn(radiusPx, canvasHeightPx - radiusPx)

            // 결과 업데이트 (Px -> Dp)
            bird.copy(
                position = Offset(
                    x = xPx.toDp().value,
                    y = yPx.toDp().value
                ),
                velocityX = newVx,
                velocityY = newVy
            )
        }
    }
}