// app/src/main/java/com/example/app/cardgame/GameSelectionScreen.kt
package com.example.app.cardgame

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(
    onGoUpDown: () -> Unit,
    onGoOldMaid: () -> Unit,
    onGoDaifugo: () -> Unit,
    onGoBlackjack: () -> Unit,
    onGoVideoPoker: () -> Unit
) {
    // 画面を縦スクロール可能に（タイトルブロック + メニュー）
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            // タイトル画面を出すため、TopAppBarは最小限表示
            CenterAlignedTopAppBar(
                title = { Text("無心トランプ", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            // --- 画像 + タイトル（無心トランプ） ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // 画像（全面に表示）
                Image(
                    painter = painterResource(id = R.drawable.index_picture),
                    contentDescription = "無心トランプ タイトル画像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // タイトルを読みやすくするためのグラデーションオーバーレイ
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.50f)
                                )
                            )
                        )
                )
                // タイトル文字（左下）
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "無心トランプ",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "シンプルに、ただ遊ぶ。",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }

            // --- メニュー（ゲーム選択） ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("ゲームを選んでください", style = MaterialTheme.typography.titleLarge)

                ElevatedCard(onClick = onGoUpDown, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Up ＆ Down（ハイ＆ロー）", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("1枚表・1枚裏 → Up/Down を当てる") }
                    )
                }

                ElevatedCard(onClick = onGoOldMaid, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("ババ抜き（Old Maid）", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("左隣から引く／初期ペア自動捨て") }
                    )
                }

                ElevatedCard(onClick = onGoDaifugo, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("大富豪", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("役あり／革命・縛り・8切り／JOKERペア対応") }
                    )
                }

                ElevatedCard(onClick = onGoBlackjack, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("ブラックジャック", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Hit/Stand／ディーラーS17／クレジット制") }
                    )
                }

                ElevatedCard(onClick = onGoVideoPoker, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("ポーカー（Video Poker: Jacks or Better）", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("5枚配布→HOLD→1回ドロー／役判定と配当") }
                    )
                }
            }
        }
    }
}