package com.example.app.cardgame

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

// Enum 名が Screen 側と一致していることを必ず確認（Betting / PlayerTurn / DealerTurn / RoundEnd / Dealt）
enum class BJPhase { Betting, PlayerTurn, DealerTurn, RoundEnd, Dealt }

data class BJState(
    val credits: Int = 100,
    val bet: Int = 10,
    val deck: List<Card> = emptyList(),
    val player: List<Card> = emptyList(),
    val dealer: List<Card> = emptyList(),   // dealer[1] は裏表示
    val phase: BJPhase = BJPhase.Betting,
    val message: String = "ベットして開始",
    val dealerReveal: Boolean = false
)

class BlackjackViewModel : ViewModel() {
    private val _ui = MutableStateFlow(BJState())
    val ui: StateFlow<BJState> = _ui

    // --- 52枚デッキ（JOKERなし） ---
    private fun newDeck52(seed: Long = System.currentTimeMillis()): MutableList<Card> {
        val list = mutableListOf<Card>()
        for (s in listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)) {
            for (r in 1..13) list += Card(s, r)
        }
        list.shuffle(Random(seed))
        return list
    }

    // --- 合計 & ソフト判定（sumOfの曖昧さを使わない） ---
    private fun value(hand: List<Card>): Pair<Int, Boolean> {
        var sum = 0
        var acesAs11 = 0
        for (c in hand) {
            val v = when (c.rank) {
                1 -> { acesAs11++; 11 }
                in 11..13 -> 10
                else -> c.rank
            }
            sum += v
        }
        while (sum > 21 && acesAs11 > 0) { sum -= 10; acesAs11-- }
        val soft = acesAs11 > 0
        return sum to soft
    }
    private fun isBlackjack(hand: List<Card>) =
        hand.size == 2 && value(hand).first == 21

    // --- ラウンド開始 ---
    fun startRound() {
        val prev = _ui.value
        require(prev.credits >= prev.bet) { "クレジット不足" }

        val deck = newDeck52()
        val p1 = deck.removeLast(); val d1 = deck.removeLast()
        val p2 = deck.removeLast(); val d2 = deck.removeLast()
        val player = listOf(p1, p2)
        val dealer = listOf(d1, d2)

        val playerBJ = isBlackjack(player)
        val dealerBJ = isBlackjack(dealer)
        val phase = if (playerBJ || dealerBJ) BJPhase.RoundEnd else BJPhase.PlayerTurn
        val reveal = (phase == BJPhase.RoundEnd)
        val message = when {
            playerBJ && dealerBJ -> "両者ブラックジャック：プッシュ"
            playerBJ -> "ブラックジャック！"
            dealerBJ -> "ディーラーブラックジャック"
            else -> "あなたの番（Hit/Stand）"
        }

        _ui.value = BJState(
            credits = prev.credits - prev.bet, // 先にベットを引く
            bet = prev.bet,
            deck = deck,
            player = player,
            dealer = dealer,
            phase = phase,
            dealerReveal = reveal,
            message = message
        )
        if (phase == BJPhase.RoundEnd) settle()
    }

    // --- プレイヤー操作 ---
    fun hit() {
        val st = _ui.value
        if (st.phase != BJPhase.PlayerTurn) return
        if (st.deck.isEmpty()) return
        val d = st.deck.toMutableList()
        val card = d.removeLast()
        val ph = st.player + card
        val pv = value(ph).first
        if (pv > 21) {
            _ui.update { it.copy(player = ph, deck = d, phase = BJPhase.DealerTurn, dealerReveal = true, message = "バースト！") }
            dealerPlay()
        } else {
            _ui.update { it.copy(player = ph, deck = d, message = "Hit/Stand どうぞ") }
        }
    }
    fun stand() {
        val st = _ui.value
        if (st.phase != BJPhase.PlayerTurn) return
        _ui.update { it.copy(phase = BJPhase.DealerTurn, dealerReveal = true) }
        dealerPlay()
    }

    // --- ディーラー（S17でスタンド） ---
    private fun dealerPlay() {
        var st = _ui.value
        var deck = st.deck.toMutableList()
        var dealer = st.dealer.toMutableList()
        while (true) {
            val (dv, _) = value(dealer)
            if (dv < 17) {
                if (deck.isEmpty()) break
                dealer += deck.removeLast()
            } else break
        }
        _ui.update { it.copy(dealer = dealer, deck = deck, dealerReveal = true) }
        settle()
    }

    // --- 精算 ---
    private fun settle() {
        val st = _ui.value
        val (pv, _) = value(st.player)
        val (dv, _) = value(st.dealer)
        val bjP = isBlackjack(st.player)
        val bjD = isBlackjack(st.dealer)
        val payoff = when {
            bjP && bjD -> st.bet
            bjP -> st.bet + (st.bet * 3) / 2
            bjD -> 0
            pv > 21 -> 0
            dv > 21 -> st.bet * 2
            pv > dv  -> st.bet * 2
            pv == dv -> st.bet
            else -> 0
        }
        val msg = "結果：あなた $pv / ディーラー $dv"
        _ui.update { it.copy(phase = BJPhase.RoundEnd, message = msg, credits = it.credits + payoff) }
    }

    fun nextRound() {
        _ui.update { BJState(credits = it.credits, bet = it.bet, message = "ベットして開始") }
    }
}