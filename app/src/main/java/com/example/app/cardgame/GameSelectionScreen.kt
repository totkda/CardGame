// app/src/main/java/com/example/app/cardgame/GameSelectionScreen.kt
package com.example.app.cardgame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(
    onGoUpDown: () -> Unit,
    onGoOldMaid: () -> Unit,
    onGoDaifugo: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("ゲーム選択") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("遊ぶゲームを選んでください", style = MaterialTheme.typography.titleLarge)

            // Up & Down
            ElevatedCard(
                onClick = onGoUpDown,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Up ＆ Down（ハイ＆ロー）", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("1枚表・1枚裏、Up/Down を当てるシンプルゲーム") }
                )
            }

            // ババ抜き
            ElevatedCard(
                onClick = onGoOldMaid,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("ババ抜き（Old Maid）", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("2〜4人、左隣から1枚引く。初期ペア自動捨て。最後の1人が敗者") }
                )
            }

            // 大富豪（プレースホルダー）
            ElevatedCard(
                onClick = onGoDaifugo,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("大富豪（準備中）", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("役判定・階段・革命など拡張予定") }
                )
            }
        }
    }
}