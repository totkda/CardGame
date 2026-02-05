package com.example.app.cardgame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

// before: fun HighLowScreen(vm: HighLowViewModel = viewModel())
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighLowScreen(
    onBack: (() -> Unit)? = null,
    vm: HighLowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Up & Down") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "戻る"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 上段（残り・スコア）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text("残り：${state.remaining}")
                Text("スコア：${state.score}", style = MaterialTheme.typography.titleLarge)
            }

            // メッセージ
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )

            // ★ 2枚並べ（左：current=表、右：next=裏→押下後に表）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.72f),
                    contentAlignment = Alignment.Center
                ) {
                    PlayingCard(card = state.current) // 常に表
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.72f),
                    contentAlignment = Alignment.Center
                ) {
                    FlippableCardFaceUpDown(
                        card = state.next,
                        faceUp = state.nextFaceUp,      // 表裏を状態で制御
                        trigger = state.flipNextKey      // フリップ再生
                    )
                }
            }

            // ボタン（判定中＝右が表の間は押せないように）
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.guess(true) },
                    enabled = !state.gameOver && state.next != null && !state.nextFaceUp
                ) { Text("Up") }

                Button(
                    onClick = { vm.guess(false) },
                    enabled = !state.gameOver && state.next != null && !state.nextFaceUp
                ) { Text("Down") }
            }

            OutlinedButton(onClick = { vm.start() }) {
                Text(if (state.gameOver) "もう一度" else "リスタート")
            }
        }
    }
}

/* --- フリップ付きカード（裏→表） --- */
@Composable
private fun FlippableCardFaceUpDown(
    card: Card?,
    faceUp: Boolean,
    trigger: Int,
    duration: Int = 300
) {
    // 表裏の切り替えに合わせて回転
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 0f else 180f, // 180°＝裏
        animationSpec = tween(durationMillis = duration),
        label = "flip"
    )

    // trigger の変更で再アニメ（ViewModel 側で flipNextKey を更新）
    LaunchedEffect(trigger) {
        // ここではアニメ制御のみ。状態は ViewModel が持つ。
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12 * density
            },
        contentAlignment = Alignment.Center
    ) {
        if (faceUp) {
            PlayingCard(card = card)          // 表
        } else {
            CardBack()                         // 裏
        }
    }
}

/* --- カード裏面（シンプルな模様） --- */
@Composable
private fun CardBack() {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, SolidColor(Color(0xFF90CAF9)), shape)
            .background(Color(0xFF1976D2), shape),
        contentAlignment = Alignment.Center
    ) {
        // 斜めの細いライン模様的なイメージ（簡易）
        Text(
            text = "♠︎♣︎♦︎♥︎",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 28.sp
        )
    }
}

/* --- カード表面：トランプ風（コーナー表記＋中央シンボル） --- */
@Composable
fun PlayingCard(card: Card?) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val shape = RoundedCornerShape(16.dp)
        val border = Color(0xFFE0E0E0)

        // スケール（単位なし）
        val h = maxHeight
        val baseHeight = 220.dp
        val scale = (h / baseHeight).coerceIn(0.8f, 1.6f)

        // 余白（Dp）
        val cornerPad = (12.dp * scale)
        val innerPad  = (6.dp  * scale)

        // 文字サイズ（Sp）
        val rankSize   = (20f * scale).sp
        val suitSize   = (16f * scale).sp
        val centerSize = (64f * scale).sp
        val centerAlpha = 0.18f

        Surface(
            color = Color.White,
            shape = shape,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, SolidColor(border), shape)
                .padding(innerPad)
        ) {
            if (card == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("—", fontSize = 42.sp)
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    // 左上コーナー
                    Column(
                        modifier = Modifier
                            .padding(cornerPad)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = rankLabel(card.rank),
                            color = suitColor(card.suit),
                            fontSize = rankSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = rankSize
                        )
                        Text(
                            text = suitLabel(card.suit),
                            color = suitColor(card.suit),
                            fontSize = suitSize
                        )
                    }
                    // 右下コーナー（180°回転）
                    Column(
                        modifier = Modifier
                            .padding(cornerPad)
                            .align(Alignment.BottomEnd)
                            .graphicsLayer { rotationZ = 180f }
                    ) {
                        Text(
                            text = rankLabel(card.rank),
                            color = suitColor(card.suit),
                            fontSize = rankSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = rankSize
                        )
                        Text(
                            text = suitLabel(card.suit),
                            color = suitColor(card.suit),
                            fontSize = suitSize
                        )
                    }
                    // 中央スート（薄め・非干渉）
                    Text(
                        text = suitLabel(card.suit),
                        color = suitColor(card.suit).copy(alpha = centerAlpha),
                        fontSize = centerSize,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = cornerPad * 1.2f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 見た目用の色（♥♦=赤 / ♣♠=黒 / JOKER=紫など任意）
fun suitColor(s: Suit): Color = when (s) {
    Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)      // 赤
    Suit.CLUBS, Suit.SPADES    -> Color(0xFF263238)      // 黒
    Suit.JOKER                 -> Color(0xFF6A1B9A)      // お好みで（紫）
}

fun suitLabel(s: Suit): String = when (s) {
    Suit.CLUBS    -> "♣"
    Suit.DIAMONDS -> "♦"
    Suit.HEARTS   -> "♥"
    Suit.SPADES   -> "♠"
    Suit.JOKER    -> "" // ここは "" でも "JOKER" でもOK（UI方針に合わせる）
}

fun rankLabel(r: Int): String = when (r) {
    0  -> "JOKER" // ★ 追加
    1  -> "A"
    11 -> "J"
    12 -> "Q"
    13 -> "K"
    else -> r.toString()
}