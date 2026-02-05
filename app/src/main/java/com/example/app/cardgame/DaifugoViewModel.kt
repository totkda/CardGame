package com.example.app.cardgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DFPhase { BotThinking, WaitingHuman, Resolving, GameOver }

data class DFPlayerSummary(
    val id: Int, val name: String, val isHuman: Boolean,
    val count: Int, val finished: Boolean, val rank: Int?
)

data class DaifugoUiState(
    val players: List<DFPlayerSummary> = emptyList(),
    val current: Int = 0,
    val myIndex: Int = 0,
    val myHand: List<Card> = emptyList(),
    val top: Play? = null,         // 現在の場（役）
    val topOwner: Int? = null,     // 直近で出した人
    val consecutivePasses: Int = 0,
    val revolution: Boolean = false,
    val suitBind: Suit? = null,
    val message: String = "",
    val phase: DFPhase = DFPhase.WaitingHuman,
    val orderOut: List<Int> = emptyList(),
    val selection: Set<Int> = emptySet() // UI: 自分の選択カード index
)

class DaifugoViewModel : ViewModel() {

    private var players = mutableListOf<DFPlayer>()
    private var currentIndex = 0
    private var orderOut = mutableListOf<Int>()
    private var top: Play? = null
    private var topOwner: Int? = null
    private var consecutivePasses = 0
    private var revolution = false
    private var suitBind: Suit? = null

    private val _ui = MutableStateFlow(DaifugoUiState())
    val ui: StateFlow<DaifugoUiState> = _ui

    fun start(numPlayers: Int = 4, mePos: Int = 0) {
        require(numPlayers == 4) { "MVPでは4人固定" }
        players = MutableList(numPlayers) { i ->
            DFPlayer(
                id = i,
                name = if (i == mePos) "あなた" else "CPU${i + 1}",
                isHuman = (i == mePos)
            )
        }
        orderOut.clear()
        top = null
        topOwner = null
        consecutivePasses = 0
        revolution = false
        suitBind = null

        // 配札（JOKERあり）
        dealDfWithJoker(players)
        players.forEach { sortDf(it.hand, revolution) }

        // 先攻：最小重みカードを持つ人
        currentIndex = players.indices.minByOrNull { p ->
            players[p].hand.minOfOrNull { dfWeight(it.rank, it.suit, revolution) } ?: Int.MAX_VALUE
        } ?: 0

        pushUi("ゲーム開始：${players[currentIndex].name} から")
        proceedTurn()
    }

    /** ---- UI連携 ---- */

    fun toggleSelectMyCard(i: Int) {
        _ui.update { st ->
            if (st.phase != DFPhase.WaitingHuman) st
            else st.copy(selection = st.selection.toMutableSet().also { s ->
                if (!s.add(i)) s.remove(i)
            })
        }
    }

    fun playSelectionHuman() {
        val st = _ui.value
        if (st.phase != DFPhase.WaitingHuman || st.current != st.myIndex) return
        val my = players[st.myIndex]
        val picked = st.selection.sorted().map { my.hand[it] }
        val play = classifySelection(picked) ?: run {
            pushUi("その組み合わせは役になりません")
            return
        }
        if (!beats(play, top, revolution, suitBind)) {
            pushUi("その役は出せません（縛り/比較NG）")
            return
        }
        // 出す
        applyPlay(st.myIndex, play)
        _ui.update { it.copy(selection = emptySet()) }
    }

    fun passHuman() {
        val st = _ui.value
        if (st.phase != DFPhase.WaitingHuman || st.current != st.myIndex) return
        onPass(st.myIndex)
    }

    /** ---- 進行管理 ---- */

    private fun pushUi(message: String) {
        val me = players.indexOfFirst { it.isHuman }.coerceAtLeast(0)
        _ui.value = DaifugoUiState(
            players = players.map {
                DFPlayerSummary(
                    id = it.id, name = it.name, isHuman = it.isHuman,
                    count = it.hand.size, finished = it.finished, rank = it.rank
                )
            },
            current = currentIndex,
            myIndex = me,
            myHand = players[me].hand.toList(),
            top = top,
            topOwner = topOwner,
            consecutivePasses = consecutivePasses,
            revolution = revolution,
            suitBind = suitBind,
            message = message,
            phase = when {
                isGameOver() -> DFPhase.GameOver
                players[currentIndex].isHuman -> DFPhase.WaitingHuman
                else -> DFPhase.BotThinking
            },
            orderOut = orderOut.toList(),
            selection = _ui.value.selection
        )
    }

    private fun isGameOver(): Boolean = players.count { !it.finished } <= 1

    private fun nextAlive(i: Int): Int {
        var j = (i + 1) % players.size
        while (players[j].finished) j = (j + 1) % players.size
        return j
    }

    private fun finishIfNeeded(i: Int) {
        val p = players[i]
        if (!p.finished && p.hand.isEmpty()) {
            p.finished = true
            p.rank = orderOut.size + 1
            orderOut += p.id
        }
    }

    private fun label(play: Play?): String = when (play) {
        null -> "（なし）"
        is Play.Single -> "${rankLabel(play.c.rank)}${suitLabel(play.c.suit)}"
        is Play.Group  -> play.cards.joinToString(" ") { "${rankLabel(it.rank)}${suitLabel(it.suit)}" }
        is Play.Run    -> play.cards.joinToString(" ") { "${rankLabel(it.rank)}${suitLabel(it.suit)}" }
    }

    /** 役を場に適用（8切り/革命/縛り更新を含む） */
    private fun applyPlay(who: Int, play: Play) {
        // 手札から除去
        val hand = players[who].hand
        playCardsRemove(hand, play)
        sortDf(hand, revolution)

        // 8切り: 即流し（top= null / topOwner = who で続行リード）
        val eightCut = containsEight(play)

        // 革命：フォー出しでトグル（Joker入りフォーも可）
        val revolutionTriggered = (play is Play.Group && play.size == 4)
        if (revolutionTriggered) {
            revolution = !revolution
            // 表示用に全員の手札並び替え
            players.forEach { sortDf(it.hand, revolution) }
        }

        // 縛り更新（場が続く場合のみ考慮）
        suitBind = if (top != null && !eightCut) updateBind(top, play, suitBind) else null

        // 場を更新
        top = if (eightCut) null else play
        topOwner = if (eightCut) who else who
        consecutivePasses = 0

        finishIfNeeded(who)
        if (isGameOver()) {
            closeGameIfNeeded()
            return
        }

        // 8切りは who のリード継続。そうでなければ次へ。
        currentIndex = if (eightCut) who else nextAlive(who)
        val msg = buildString {
            append("${players[who].name} が［${label(play)}］を出しました")
            if (revolutionTriggered) append(" ／ 革命発生！")
            if (eightCut) append(" ／ 8切りで場流し")
            if (suitBind != null && top != null) append(" ／ 縛り：${suitLabel(suitBind!!)}")
        }
        pushUi(msg)
        proceedTurn()
    }

    private fun playCardsRemove(hand: MutableList<Card>, play: Play) {
        fun removeCard(c: Card) {
            val idx = hand.indexOfFirst { it.rank == c.rank && it.suit == c.suit }
            if (idx >= 0) hand.removeAt(idx)
        }
        when (play) {
            is Play.Single -> removeCard(play.c)
            is Play.Group  -> play.cards.forEach { removeCard(it) }
            is Play.Run    -> play.cards.forEach { removeCard(it) }
        }
    }

    private fun closeGameIfNeeded() {
        if (!isGameOver()) return
        val last = players.indexOfFirst { !it.finished }
        if (last >= 0 && players[last].rank == null) {
            players[last].rank = orderOut.size + 1
            orderOut += players[last].id
        }
        pushUi("ゲーム終了")
    }

    /** パス処理 */
    private fun onPass(who: Int) {
        consecutivePasses++
        val alive = players.count { !it.finished }
        if (topOwner != null && consecutivePasses >= alive - 1) {
            // 場流し：最後に出した人がリード
            currentIndex = topOwner!!
            top = null
            topOwner = null
            consecutivePasses = 0
            suitBind = null
            pushUi("場が流れました。${players[currentIndex].name} から")
            proceedTurn()
            return
        }
        currentIndex = nextAlive(currentIndex)
        pushUi("${players[who].name} はパス")
        proceedTurn()
    }

    /** ターン進行（CPU含む） */
    private fun proceedTurn() {
        closeGameIfNeeded()
        if (isGameOver()) return

        val cur = currentIndex
        if (players[cur].isHuman) {
            pushUi("役を選んで「出す」か、パスを押してください。")
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(phase = DFPhase.BotThinking) }
            delay(450)

            val hand = players[cur].hand
            // ---- 候補列挙（弱い順） ----
            val candidates = enumerateCandidates(hand, revolution)
                .filter { beats(it, top, revolution, suitBind) }
                .sortedWith(compareBy({ strengthKey(it, revolution) }, { it.type.ordinal }))

            if (candidates.isEmpty()) {
                onPass(cur)
            } else {
                val chosen = chooseWithSimpleHeuristics(candidates)
                applyPlay(cur, chosen)
            }
        }
    }

    /** 候補役の列挙（Single/Group/Run・Jokerペア対応） */
    private fun enumerateCandidates(hand: List<Card>, rev: Boolean): List<Play> {
        val out = mutableListOf<Play>()
        val nonJ = hand.filter { it.suit != Suit.JOKER }
        val jokers = hand.filter { it.suit == Suit.JOKER }

        // Single
        hand.forEach { out += Play.Single(it) }

        // Group（同ランク 2..4）＋ Joker混ぜ
        val byRank = nonJ.groupBy { it.rank }
        for ((r, cards) in byRank) {
            // 既存だけで 2..4
            if (cards.size >= 2) out += Play.Group(cards.take(2))
            if (cards.size >= 3) out += Play.Group(cards.take(3))
            if (cards.size >= 4) out += Play.Group(cards.take(4))
            // Joker でペア補完
            if (cards.size == 1 && jokers.isNotEmpty()) out += Play.Group(listOf(cards.first(), jokers.first()))
        }

        // Run（同スート 連番>=3）※ Joker不可
        val bySuit = nonJ.groupBy { it.suit }
        for ((s, cs) in bySuit) {
            val sorted = cs.sortedBy { dfWeightBase(it.rank, it.suit) }
            // しっかり列挙：スライドしながら最長連結を抽出
            var i = 0
            while (i < sorted.size) {
                var j = i
                while (j + 1 < sorted.size && sorted[j + 1].rank == sorted[j].rank + 1) j++
                val len = j - i + 1
                if (len >= 3) {
                    // 最短3で全ての部分列（3..len）を候補に
                    for (k in 3..len) {
                        for (start in i..(j - k + 1)) {
                            out += Play.Run(sorted.subList(start, start + k), s)
                        }
                    }
                }
                i = j + 1
            }
        }
        return out
    }

    /** 役の“弱さキー”（小さいほど弱い＝CPUは基本これを選ぶ） */
    private fun strengthKey(p: Play, rev: Boolean): Int = when (p) {
        is Play.Single -> dfWeight(p.c.rank, p.c.suit, rev)
        is Play.Group  -> {
            val nonJ = p.cards.firstOrNull { it.suit != Suit.JOKER } ?: p.cards.first()
            dfWeight(nonJ.rank, nonJ.suit, rev) + (p.cards.size * 16)
        }
        is Play.Run    -> {
            val top = p.cards.maxBy { it.rank }
            dfWeight(top.rank, top.suit, rev) + (p.size * 12)
        }
    }

    /** 単純ヒューリスティクス：8切りや革命が確定する札は温存し、最小勝ちを選びがち */
    private fun chooseWithSimpleHeuristics(cands: List<Play>): Play {
        // 8切り・革命をできるだけ温存
        val safe = cands.filterNot { containsEight(it) || (it is Play.Group && it.size == 4) }
        return safe.minByOrNull { strengthKey(it, revolution) } ?: cands.minBy { strengthKey(it, revolution) }
    }
}