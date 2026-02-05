package com.example.app.cardgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldMaidScreen(
    onBack: (() -> Unit)? = null,
    vm: OldMaidViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.start(numPlayers = 3, mePos = 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ババ抜き") },
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上段：プレイヤー概要
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.players.forEach { p ->
                    val highlight = p.id == state.current
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${p.name}：${p.count}",
                                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        enabled = !p.finished,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (highlight) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        )
                    )
                }
            }

            // 中段：現在手番者の左隣（引き取り先）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val leftName = state.players.firstOrNull { it.id == state.leftNeighborOfCurrent }?.name ?: "左隣"
                    Text("引く相手：$leftName（裏向き ${state.neighborBackCount} 枚）")

                    // 裏カード群（ヒトの手番のみクリック可）
                    val scroll = rememberScrollState()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(state.neighborBackCount) { idx ->
                            CardBackSmall(
                                enabled = state.phase == OldMaidPhase.WaitingForHumanPick,
                                onClick = { vm.onHumanPick(idx) }
                            )
                        }
                        if (state.neighborBackCount == 0) {
                            Text("（引くカードなし）", color = Color.Gray)
                        }
                    }
                }
            }

            // 下段：自分の手札（表表示）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("あなたの手札（自動でペアは捨てられます）")
                    val scroll = rememberScrollState()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.myHand.forEach { c ->
                            PlayingCardMini(c)
                        }
                        if (state.myHand.isEmpty()) {
                            Text("（なし）", color = Color.Gray)
                        }
                    }
                }
            }

            // メッセージ
            Text(state.message, fontSize = 14.sp)

            // リスタート
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { vm.start() }) { Text("リスタート") }
            }

            // ゲーム終了時の順位表示
            if (state.phase == OldMaidPhase.GameOver) {
                Divider()
                Text("結果", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 上がった順（最後に残ったのが敗者）
                    state.orderOut.forEachIndexed { idx, id ->
                        val name = state.players.first { it.id == id }.name
                        Text("${idx + 1} 位：$name")
                    }
                    // 敗者
                    val losers = state.players.filter { it.id !in state.orderOut.map { id -> id } }
                    losers.forEach { l ->
                        Text("敗者：${l.name}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

/* ----------------- UI 部品 ----------------- */

@Composable private fun CardBackSmall(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 54.dp)
            .border(1.dp, SolidColor(Color(0xFF90CAF9)), shape)
            .background(if (enabled) Color(0xFF1976D2) else Color(0xFF90A4AE), shape)
            .let { m -> if (enabled) m.clickable { onClick() } else m },
        contentAlignment = Alignment.Center
    ) {
        Text("♠︎", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

@Composable private fun PlayingCardMini(card: Card) {
    val shape = RoundedCornerShape(8.dp)
    val border = Color(0xFFE0E0E0)
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        Suit.CLUBS, Suit.SPADES -> Color(0xFF263238)
        Suit.JOKER -> Color(0xFF6A1B9A)
    }
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 80.dp)
            .border(1.dp, SolidColor(border), shape)
            .background(Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (card.suit == Suit.JOKER) {
            Text("JOKER", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rankLabel(card.rank), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(suitLabel(card.suit), color = color, fontSize = 14.sp)
            }
        }
    }
}