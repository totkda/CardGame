package com.example.app.cardgame

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackjackScreen(onBack: (() -> Unit)? = null, vm: BlackjackViewModel = viewModel()) {
    val st by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ブラックジャック") },
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack){ Icon(Icons.Filled.ArrowBack,"戻る") } }
            )
        }
    ){ padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            // クレジット／ベット
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("クレジット：${st.credits}")
                Text("ベット：${st.bet}")
            }

            // ディーラー
            Text("ディーラー")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (st.dealer.isEmpty()) {
                    CardBackSmall(enabled = false, onClick = {})
                } else {
                    st.dealer.forEachIndexed { i, c ->
                        val hide = (i == 1 && !st.dealerReveal)
                        if (hide) CardBackSmall(enabled = false, onClick = {}) else PlayingCardMini(c)
                    }
                }
            }


            // プレイヤー
            Text("あなた")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                st.player.forEach { PlayingCardMini(it) }
            }

            Text(st.message)

            // 操作
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (st.phase) {
                    BJPhase.Betting -> {
                        Button(onClick = { vm.startRound() }) { Text("ディール") }
                    }
                    BJPhase.PlayerTurn -> {
                        Button(onClick = { vm.hit() }) { Text("Hit") }
                        Button(onClick = { vm.stand() }) { Text("Stand") }
                    }
                    BJPhase.DealerTurn -> {
                        CircularProgressIndicator()
                    }
                    BJPhase.RoundEnd -> {
                        Button(onClick = { vm.nextRound() }) { Text("次のラウンド") }
                    }
                    BJPhase.Dealt -> { /* 使わない状態だが網羅のため残す */ }
                }
            }
        }
    }
}