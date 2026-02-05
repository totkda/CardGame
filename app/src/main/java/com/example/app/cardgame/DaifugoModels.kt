package com.example.app.cardgame

/** プレイヤー情報 */
data class DFPlayer(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val hand: MutableList<Card> = mutableListOf(),
    var finished: Boolean = false,
    var rank: Int? = null
)

/** 役種 */
enum class PlayType { Single, Group, Run }

/** 出し手（役の実体） */
sealed class Play(val type: PlayType) {
    class Single(val c: Card) : Play(PlayType.Single)
    class Group(val cards: List<Card>) : Play(PlayType.Group) { val size = cards.size }
    class Run(val cards: List<Card>, val suit: Suit) : Play(PlayType.Run) { val size = cards.size }
}

/** 大富豪の重み（JOKER最強=16, 2=15, A=14, 3..K=3..13） */
fun dfWeightBase(rank: Int, suit: Suit): Int = when {
    suit == Suit.JOKER || rank == 0 -> 16
    rank == 2 -> 15
    rank == 1 -> 14
    else -> rank // 3..13
}

/** 革命反転後の重み（JOKERは最強のまま） */
fun dfWeight(rank: Int, suit: Suit, revolution: Boolean): Int {
    val base = dfWeightBase(rank, suit)
    return if (!revolution) base else when (base) {
        16 -> 16 // Joker stays top
        else -> 18 - base // 15<->3,14<->4,... を対称反転
    }
}

/** 手牌の並び替え（見やすさ用） */
fun sortDf(hand: MutableList<Card>, revolution: Boolean) {
    hand.sortWith(compareBy({ dfWeight(it.rank, it.suit, revolution) }, { it.suit.ordinal }))
}

/** 53枚（JOKER1枚）を配札 */
fun dealDfWithJoker(players: MutableList<DFPlayer>) {
    val deck = fullDeckWithJoker()  // 52 + JOKER = 53
    deck.shuffle(kotlin.random.Random(System.currentTimeMillis()))
    var i = 0
    while (deck.isNotEmpty()) {
        players[i % players.size].hand += deck.removeLast()
        i++
    }
}

/** ---- 役の判定・比較 ---- */

/** 選択カードを役に分類。JOKERは「ペアのみ」ワイルド可。階段では不可。 */
fun classifySelection(sel: List<Card>): Play? {
    if (sel.isEmpty()) return null
    val nonJoker = sel.filter { it.suit != Suit.JOKER && it.rank != 0 }
    val jokerCount = sel.size - nonJoker.size

    // Single
    if (sel.size == 1) return Play.Single(sel.first())

    // Group（同ランク 2..4）
    val allSameRank = nonJoker.map { it.rank }.distinct().size == 1
    if (allSameRank && sel.size in 2..4) {
        // Joker を混ぜても OK（ペア/スリー/フォー形成可）
        return Play.Group(sel.sortedBy { dfWeightBase(it.rank, it.suit) })
    }
    // Joker でペア作成（JOKER + X）
    if (sel.size == 2 && jokerCount == 1 && nonJoker.size == 1) {
        return Play.Group(sel) // 擬似ペア（Xのペアとして扱う）
    }

    // Run（同一スート・連番3枚以上）※ Jokerは不可
    if (jokerCount == 0) {
        val sameSuit = nonJoker.map { it.suit }.distinct().size == 1
        if (sameSuit && sel.size >= 3) {
            val suit = nonJoker.first().suit
            val ranks = nonJoker.map { it.rank }.sorted()
            // A-2-3 や 2-A-K の循環は不可：純粋な昇順のみ
            val consecutive = ranks.zipWithNext().all { (a, b) -> b == a + 1 }
            if (consecutive) return Play.Run(nonJoker, suit)
        }
    }
    return null
}

/** 役の強さ比較：base を上回れるか（縛り・革命・同型/同枚数チェック込み） */
fun beats(candidate: Play, base: Play?, revolution: Boolean, suitBind: Suit?): Boolean {
    // 縛り：候補が縛りスートか？
    fun obeysBind(play: Play): Boolean = when (play) {
        is Play.Single -> suitBind == null || play.c.suit == suitBind
        is Play.Group  -> suitBind == null || play.cards.all { it.suit == suitBind }
        is Play.Run    -> suitBind == null || play.suit == suitBind
    }
    if (!obeysBind(candidate)) return false
    if (base == null) return true
    if (candidate.type != base.type) return false

    return when (candidate) {
        is Play.Single -> {
            val b = (base as Play.Single).c
            val cw = dfWeight(candidate.c.rank, candidate.c.suit, revolution)
            val bw = dfWeight(b.rank, b.suit, revolution)
            cw > bw
        }
        is Play.Group -> {
            val b = base as Play.Group
            if (candidate.size != b.size) return false
            // Joker含みペアは「非Jokerのランク」を比較基準にする
            fun key(cards: List<Card>): Int {
                val nonJ = cards.firstOrNull { it.suit != Suit.JOKER } ?: cards.first()
                return dfWeight(nonJ.rank, nonJ.suit, revolution)
            }
            key(candidate.cards) > key(b.cards)
        }
        is Play.Run -> {
            val b = base as Play.Run
            if (candidate.size != b.size) return false
            // 末尾カードの重みで比較（双方同スートが前提）
            val cTop = candidate.cards.maxBy { it.rank }
            val bTop = b.cards.maxBy { it.rank }
            dfWeight(cTop.rank, cTop.suit, revolution) > dfWeight(bTop.rank, bTop.suit, revolution)
        }
    }
}

/** プレイに 8 が含まれるか（8切り） */
fun containsEight(play: Play): Boolean = when (play) {
    is Play.Single -> play.c.rank == 8
    is Play.Group  -> play.cards.any { it.rank == 8 }
    is Play.Run    -> play.cards.any { it.rank == 8 }
}

/** 縛り更新：直前と今回が同スートなら発動/継続、違えば解除。 */
fun updateBind(prev: Play?, now: Play?, currentBind: Suit?): Suit? {
    if (prev == null || now == null) return null
    fun suitOf(p: Play): Suit? = when (p) {
        is Play.Single -> p.c.suit
        is Play.Run    -> p.suit
        is Play.Group  -> if (p.cards.all { it.suit == p.cards.first().suit }) p.cards.first().suit else null
    }
    val sPrev = suitOf(prev) ?: return null
    val sNow  = suitOf(now) ?: return null
    return if (sPrev == sNow) sNow else null
}