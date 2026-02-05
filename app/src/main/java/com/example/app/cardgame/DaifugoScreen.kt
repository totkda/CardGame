package com.example.app.cardgame


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.ui.draw.rotate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaifugoScreen(
    onBack: (() -> Unit)? = null,
    vm: DaifugoViewModel = viewModel()
) {
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.start(numPlayers = 4, mePos = 0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val rev = if (state.revolution) "（革命中）" else ""
                    val bind = state.suitBind?.let { "／縛り:${suitLabel(it)}" } ?: ""
                    Text("大富豪$rev$bind")
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "戻る") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上段：プレイヤー状況
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.players.forEach { p ->
                    val hl = p.id == state.current
                    AssistChip(
                        onClick = {},
                        label = { Text("${p.name}：${p.count}${p.rank?.let { " / ${it}位" } ?: ""}") },
                        enabled = !p.finished,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (hl) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        )
                    )
                }
            }

            // --- 中段：場（置き換え） ---
            FieldArea(
                top = state.top,
                topOwnerName = state.topOwner?.let { id -> state.players.firstOrNull { it.id == id }?.name } ?: "－",
                revolution = state.revolution,
                suitBind = state.suitBind,
                consecutivePasses = state.consecutivePasses
            )

            // 下段：あなたの手札（タップで複数選択 → 出す）
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("あなたの手札（タップで選択 → 出す）")
                    val scroll = rememberScrollState()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.myHand.forEachIndexed { idx, c ->
                            PlayingCardSelectable(
                                card = c,
                                selected = idx in state.selection,
                                enabled = state.phase == DFPhase.WaitingHuman && state.current == state.myIndex,
                                onClick = { vm.toggleSelectMyCard(idx) }
                            )
                        }
                        if (state.myHand.isEmpty()) Text("（なし）", color = Color.Gray)
                    }
                }
            }

            // メッセージ
            Text(state.message)

            // アクション
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.playSelectionHuman() },
                    enabled = state.phase == DFPhase.WaitingHuman && state.selection.isNotEmpty()
                ) { Text("出す") }

                OutlinedButton(
                    onClick = { vm.passHuman() },
                    enabled = state.phase == DFPhase.WaitingHuman
                ) { Text("パス") }

                OutlinedButton(onClick = { vm.start() }) { Text("リスタート") }
            }

            // 結果
            if (state.phase == DFPhase.GameOver) {
                Divider()
                Text("結果", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.orderOut.forEachIndexed { i, id ->
                        val name = state.players.first { it.id == id }.name
                        Text("${i + 1} 位：$name")
                    }
                }
            }
        }
    }
}

/* 小さめカード（選択状態表示付き） */
@Composable
private fun PlayingCardSelectable(card: Card, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val border = if (selected) Color(0xFF42A5F5) else Color(0xFFE0E0E0)
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        Suit.CLUBS, Suit.SPADES    -> Color(0xFF263238)
        Suit.JOKER                 -> Color(0xFF6A1B9A)
    }
    Box(
        modifier = Modifier
            .size(54.dp, 80.dp)
            .border(2.dp, SolidColor(border), shape)
            .background(Color.White, shape)
            .let { m -> if (enabled) m.clickable(onClick = onClick) else m },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rankLabel(card.rank), color = color, fontSize = 16.sp)
            Text(suitLabel(card.suit), color = color, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FieldArea(
    top: Play?,
    topOwnerName: String,
    revolution: Boolean,
    suitBind: Suit?,
    consecutivePasses: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (revolution) Color(0xFFFFF3E0) else Color(0xFFFAFAFA) // 革命時は少し色を変える
        )
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1) ステータス行：役種 / 革命 / 縛り / 連続パス
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {}, label = { Text("場") })
                if (revolution) {
                    AssistChip(
                        onClick = {},
                        label = { Text("革命中", color = Color(0xFFBF360C)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFE0B2))
                    )
                }
                if (suitBind != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text("縛り: ${suitLabel(suitBind)}") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE3F2FD))
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("連続パス: $consecutivePasses") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF5F5F5))
                )
                Spacer(Modifier.weight(1f))
                Text("出し手：$topOwnerName", fontSize = 12.sp, color = Color.Gray)
            }

            // 2) 場のカード本体（アニメ付きで見やすく）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = top,
                    transitionSpec = {
                        // フェードのみ（軽量）
                        fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 180))

                    },
                    label = "fieldAnim"
                ) { play ->
                    when (play) {
                        null -> Text("（なし）", color = Color.Gray)
                        is Play.Single -> FieldSingle(play.c)
                        is Play.Group  -> FieldGroup(play.cards)
                        is Play.Run    -> FieldRun(play.cards)
                    }
                }
            }
        }
    }
}

// --- Single：大きめ1枚 ---
@Composable
private fun FieldSingle(card: Card) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PlayingCardBig(card)
    }
}

// --- Group：扇形配置（軽く回転） ---
@Composable
private fun FieldGroup(cards: List<Card>) {
    val spread = (cards.size - 1).coerceAtLeast(0)
    val baseAngle = 10f // 角度差
    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy((-24).dp), verticalAlignment = Alignment.CenterVertically) {
            cards.forEachIndexed { i, c ->
                val angle = (i - spread / 2f) * baseAngle
                Box(
                    modifier = Modifier
                        .size(80.dp, 118.dp)
                        .rotate(angle)
                ) {
                    PlayingCardMedium(c)
                }
            }
        }
    }
}

// --- Run：横に並べる ---
@Composable
private fun FieldRun(cards: List<Card>) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cards.forEach { PlayingCardMedium(it) }
    }
}

// --- 共通：場用のカード（大/中の2サイズ） ---
@Composable
private fun PlayingCardBig(card: Card) {
    val shape = RoundedCornerShape(16.dp)
    val border = Color(0xFFE0E0E0)
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        Suit.CLUBS, Suit.SPADES -> Color(0xFF263238)
        Suit.JOKER -> Color(0xFF6A1B9A)
    }
    Box(
        modifier = Modifier
            .size(110.dp, 160.dp)
            .border(1.dp, SolidColor(border), shape)
            .background(Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rankLabel(card.rank), color = color, fontSize = 28.sp)
            Text(suitLabel(card.suit), color = color, fontSize = 22.sp)
        }
    }
}

@Composable
private fun PlayingCardMedium(card: Card) {
    val shape = RoundedCornerShape(12.dp)
    val border = Color(0xFFE0E0E0)
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        Suit.CLUBS, Suit.SPADES -> Color(0xFF263238)
        Suit.JOKER -> Color(0xFF6A1B9A)
    }
    Box(
        modifier = Modifier
            .size(80.dp, 118.dp)
            .border(1.dp, SolidColor(border), shape)
            .background(Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rankLabel(card.rank), color = color, fontSize = 22.sp)
            Text(suitLabel(card.suit), color = color, fontSize = 18.sp)
        }
    }
}
