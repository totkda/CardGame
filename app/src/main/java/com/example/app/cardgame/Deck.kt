package com.example.app.cardgame

class Deck(seed: Long? = null) {

    // ▼ 再現性ありのシャッフルにする場合は Kotlin Random を使う
    private val rng: kotlin.random.Random =
        seed?.let { kotlin.random.Random(it) } ?: kotlin.random.Random(System.currentTimeMillis())

    // ▼ デッキ（JOKERなし：Up&Down）
    private val cards: MutableList<Card> =
        buildList<Card> {
            for (s in listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)) {
                for (r in 1..13) add(Card(s, r))
            }
        }
            .toMutableList()
            .also { it.shuffle(rng) }   // ← Kotlin Random を渡す

    fun draw(): Card? =
        if (cards.isNotEmpty()) cards.removeAt(cards.lastIndex) else null

    fun size(): Int = cards.size
}