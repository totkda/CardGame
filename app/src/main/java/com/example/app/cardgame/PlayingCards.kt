// app/src/main/java/com/example/app/cardgame/PlayingCards.kt
package com.example.app.cardgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ── ここに “公開” の共通カード部品をまとめておく ── **/

@Composable
fun PlayingCardMini(
    card: Card,
    enabled: Boolean = false,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(8.dp)
    val border = Color(0xFFE0E0E0)
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F) // 赤
        Suit.CLUBS,  Suit.SPADES   -> Color(0xFF263238) // 黒
        Suit.JOKER                 -> Color(0xFF6A1B9A) // 紫
    }
    Box(
        modifier = Modifier
            .size(54.dp, 80.dp)
            .border(1.dp, SolidColor(border), shape)
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

@Composable
fun CardBackSmall(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(36.dp, 54.dp)
            .border(1.dp, SolidColor(Color(0xFF90CAF9)), shape)
            .background(if (enabled) Color(0xFF1976D2) else Color(0xFF90A4AE), shape)
            .let { m -> if (enabled) m.clickable(onClick = onClick) else m },
        contentAlignment = Alignment.Center
    ) {
        Text("♠︎", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}