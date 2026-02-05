// app/src/main/java/com/example/app/cardgame/OldMaidModels.kt
package com.example.app.cardgame

data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val hand: MutableList<Card> = mutableListOf(),
    var finished: Boolean = false
)

/** 52枚＋JOKER */
fun fullDeckWithJoker(): MutableList<Card> {
    val list = mutableListOf<Card>()
    for (s in listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)) {
        for (r in 1..13) list += Card(s, r)
    }
    list += Card(Suit.JOKER, 0) // Joker 1枚
    return list
}

/** 同ランク2枚のペアを全除去（JOKERは除去対象外） */
fun removePairsAll(hand: MutableList<Card>) {
    if (hand.isEmpty()) return
    val groups = hand.filter { it.rank != 0 }.groupBy { it.rank }
    val toRemove = mutableListOf<Card>()
    for ((_, cards) in groups) {
        val pairs = (cards.size / 2) * 2
        toRemove += cards.take(pairs)
    }
    hand.removeAll(toRemove.toSet())
}