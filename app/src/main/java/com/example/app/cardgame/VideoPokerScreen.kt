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
fun VideoPokerScreen(onBack: (() -> Unit)? = null, vm: VideoPokerViewModel = viewModel()) {
    val st by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ポーカー（Video Poker: Jacks or Better）") },
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack){ Icon(Icons.Filled.ArrowBack,"戻る") } }
            )
        }
    ){ padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("クレジット：${st.credits}")
                Text("ベット：${st.bet}")
            }

            // 手牌（HOLD 表示付き）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                st.hand.forEachIndexed { i, c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayingCardMini(c,
                            enabled = st.phase == VPPhase.Holding,
                            onClick = { vm.toggleHold(i) }
                        )
                        val held = i in st.holds
                        Text(if (held) "HOLD" else " ", color = if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(st.message)
            if (st.resultLabel.isNotBlank()) Text("役：${st.resultLabel}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (st.phase) {
                    VPPhase.Betting -> Button(onClick = { vm.deal() }) { Text("ディール") }
                    VPPhase.Holding -> {
                        Button(onClick = { vm.draw() }) { Text("ドロー") }
                    }
                    VPPhase.Drawn, VPPhase.RoundEnd -> Button(onClick = { vm.nextRound() }) { Text("次のラウンド") }
                    VPPhase.Dealt -> {}
                }
            }
        }
    }
}