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
    onGoDaifugo: () -> Unit,
    onGoBlackjack: () -> Unit,
    onGoVideoPoker: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("ゲーム選択") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("遊ぶゲームを選んでください", style = MaterialTheme.typography.titleLarge)

            ElevatedCard(onClick = onGoUpDown, modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("Up ＆ Down（ハイ＆ロー）") },
                    supportingContent = { Text("表1枚・裏1枚 → Up/Down 判定") })
            }
            ElevatedCard(onClick = onGoOldMaid, modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("ババ抜き（Old Maid）") },
                    supportingContent = { Text("左隣から引く／初期ペア自動捨て") })
            }
            ElevatedCard(onClick = onGoDaifugo, modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("大富豪") },
                    supportingContent = { Text("役あり／革命・縛り・8切り・JOKERペア対応") })
            }

            // ブラックジャック
            ElevatedCard(onClick = onGoBlackjack, modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("ブラックジャック") },
                    supportingContent = { Text("Hit/Stand／ディーラーS17／クレジット制") })
            }

            // ビデオポーカー（Jacks or Better）
            ElevatedCard(onClick = onGoVideoPoker, modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("ポーカー（Video Poker: Jacks or Better）") },
                    supportingContent = { Text("5枚配布→HOLD→引き直し／配当") })
            }
        }
    }
}