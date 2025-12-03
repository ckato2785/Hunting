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

enum class BirdType(
    val description: String,
    val score: Int,
    val drawableId: Int,
    val baseSizeDp: Dp = 60.dp, // 기본 크기
    val sizeFactor: Float, // 크기 배율
    val maxCount: Int // 화면 최대 스폰 개수
) {
    SPARROW( // 참새: 주요 목표 (+5점), 4마리 스폰
        description = "참새 (+5점)",
        score = 5,
        drawableId = R.drawable.ckato,
        sizeFactor = 1.0f,
        maxCount = 4
    ),
    BUNTING( // 멧새: 감점 (-4점), 2마리 스폰
        description = "멧새 (-5점)",
        score = -5,
        drawableId = R.drawable.aptto,
        sizeFactor = 1.0f,
        maxCount = 2
    ),
    MAGPIE( // 까치: 감점 (-2점), 3마리 스폰, 조금 더 크게
        description = "까치 (-2점)",
        score = -2,
        drawableId = R.drawable.magpie,
        sizeFactor = 1.4f,
        maxCount = 3
    );

    // 실제 화면에 표시될 Dp 크기를 계산
    val actualSizeDp: Dp
        get() = baseSizeDp * sizeFactor
}

data class Bird(
    val id: Int,
    var position: Offset,
    val type: BirdType,
    val sizeDp: Dp,
    val creationTime: Long = System.currentTimeMillis(),
    val velocityX: Float = 0f,
    val velocityY: Float = 0f
)

class GameState(
    initialBirds: List<Bird> = emptyList()
) {
    var birds by mutableStateOf(initialBirds)
    var score by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)
    var isGameClear by mutableStateOf(false)
    var timeLeft by mutableStateOf(60)
}

const val CLEAR_SCORE = 100 // 숫자 변경시 클리어 점수 늘어남
const val MAX_TOTAL_BIRDS = 9 // 참새(4) + 멧새(2) + 까치(3) = 9

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BirdGameScreen() {
    val gameState = remember { GameState() }
    var showClearDialog by remember { mutableStateOf(false) }
    LaunchedEffect(gameState.isGameOver, gameState.isGameClear) {
        if (!gameState.isGameOver && !gameState.isGameClear && gameState.timeLeft > 0) {
            while (true) {
                delay(1000L)
                gameState.timeLeft--


                if (gameState.score >= CLEAR_SCORE) {
                    gameState.isGameClear = true
                    showClearDialog = true
                    break
                }


                if (gameState.timeLeft == 0) {
                    gameState.isGameOver = true
                    break
                }


                val currentTime = System.currentTimeMillis()
                gameState.birds = gameState.birds.filter {
                    currentTime - it.creationTime < 3000
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
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
                LaunchedEffect(key1 = gameState.isGameOver, key2 = gameState.isGameClear) {
                    if (!gameState.isGameOver && !gameState.isGameClear) {
                        while (true) {
                            delay(16) // 약 60 FPS
                            val currentBirds = gameState.birds
                            if (currentBirds.size < MAX_TOTAL_BIRDS && Random.nextFloat() < 0.1f) {
                                val availableTypes = BirdType.entries.filter { type ->
                                    currentBirds.count { it.type == type } < type.maxCount
                                }

                                if (availableTypes.isNotEmpty()) {
                                    val typeToSpawn = availableTypes.random()
                                    val newBird = makeNewBird(maxWidth, maxHeight, typeToSpawn)
                                    gameState.birds = currentBirds + newBird
                                }
                            }
                            gameState.birds = updateBirdPositions(
                                gameState.birds,
                                canvasWidthPx,
                                canvasHeightPx,
                                density
                            )
                        }
                    }
                }
                gameState.birds.forEach { bird ->
                    BirdComposable(bird = bird) {
                        gameState.score += bird.type.score
                        gameState.birds =
                            gameState.birds.filterNot { it.id == bird.id }
                    }
                }
            }
        }
        if (showClearDialog) {
            GameClearDialog(
                score = gameState.score,
                onRestart = {
                    showClearDialog = false
                    restartGame(gameState)
                },
                onExit = {}
            )
        }

        // 게임 오버 표시하기
        if (gameState.isGameOver && !gameState.isGameClear) {
            GameOverDialog(
                score = gameState.score,
                onRestart = { restartGame(gameState) },
                onExit = { }
            )
        }
    }
}

// birdcomposable
// 새를 그리는 코드
@Composable
fun BirdComposable(bird: Bird, onClick: () -> Unit) {
    val birdSizeDp = bird.sizeDp

    Image(
        painter = painterResource(id = bird.type.drawableId),
        contentDescription = "Bird: ${bird.type.description}",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(birdSizeDp)
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


@Composable
fun GameClearDialog(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("🎉 스테이지 클리어! 🎉") },
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

@Composable
fun GameOverDialog(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("클리어 실패!") },
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

@Composable
fun GameStatusRow(score: Int, timeLeft: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Score: $score / $CLEAR_SCORE", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Time: ${timeLeft}s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun makeNewBird(maxWidth: Dp, maxHeight: Dp, birdType: BirdType): Bird {
    val sizeDp = birdType.actualSizeDp
    val radiusDp = sizeDp / 2

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

        velocityX = (Random.nextFloat() * 2 + 1) * if (Random.nextBoolean()) 1f else -1f,
        velocityY = (Random.nextFloat() * 2 + 1) * if (Random.nextBoolean()) 1f else -1f
    )
}


fun restartGame(gameState: GameState) {
    gameState.score = 0
    gameState.timeLeft = 60
    gameState.isGameOver = false
    gameState.isGameClear = false
    gameState.birds = emptyList()
}



fun updateBirdPositions(
    birds: List<Bird>,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    density: Density
): List<Bird> {
    return birds.map { bird ->
        with(density) {

            val radiusDp = bird.sizeDp / 2

            val radiusPx = radiusDp.toPx()


            var xPx = bird.position.x.dp.toPx()
            var yPx = bird.position.y.dp.toPx()


            val vxPx = bird.velocityX.dp.toPx()
            val vyPx = bird.velocityY.dp.toPx()


            xPx += vxPx
            yPx += vyPx

            var newVx = bird.velocityX
            var newVy = bird.velocityY


            if (xPx < radiusPx) {
                newVx = abs(newVx)
            } else if (xPx > canvasWidthPx - radiusPx) {
                newVx = -abs(newVx)
            }

            if (yPx < radiusPx) {
                newVy = abs(newVy)
            } else if (yPx > canvasHeightPx - radiusPx) {
                newVy = -abs(newVy)
            }


            xPx = xPx.coerceIn(radiusPx, canvasWidthPx - radiusPx)
            yPx = yPx.coerceIn(radiusPx, canvasHeightPx - radiusPx)


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