package com.example.app.cardgame

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class VPPhase { Betting, Dealt, Holding, Drawn, RoundEnd }

data class VPState(
    val credits: Int = 100,
    val bet: Int = 5,
    val deck: List<Card> = emptyList(),
    val hand: List<Card> = emptyList(),
    val holds: Set<Int> = emptySet(),
    val phase: VPPhase = VPPhase.Betting,
    val message: String = "ベットしてディール",
    val resultLabel: String = ""
)

class VideoPokerViewModel : ViewModel() {
    private val _ui = MutableStateFlow(VPState())
    val ui: StateFlow<VPState> = _ui

    private fun newDeck52(seed: Long = System.currentTimeMillis()): MutableList<Card> {
        val list = mutableListOf<Card>()
        for (s in listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES))
            for (r in 1..13) list += Card(s, r)
        list.shuffle(Random(seed)); return list
    }

    fun deal() {
        val st = _ui.value
        require(st.credits >= st.bet) { "クレジット不足" }
        val d = newDeck52()
        val h = List(5) { d.removeLast() }
        _ui.value = VPState(
            credits = st.credits - st.bet,
            bet = st.bet,
            deck = d,
            hand = h,
            holds = emptySet(),
            phase = VPPhase.Holding,
            message = "保持(HOLD)するカードをタップ → 『ドロー』"
        )
    }

    fun toggleHold(i: Int) {
        val st = _ui.value
        if (st.phase != VPPhase.Holding) return
        val s = st.holds.toMutableSet()
        if (!s.add(i)) s.remove(i)
        _ui.update { it.copy(holds = s) }
    }

    fun draw() {
        val st = _ui.value
        if (st.phase != VPPhase.Holding) return
        val d = st.deck.toMutableList()
        val newHand = st.hand.mapIndexed { idx, c -> if (idx in st.holds) c else d.removeLast() }
        val (label, pay) = evaluateAndPayout(newHand, st.bet)
        _ui.update { it.copy(hand = newHand, deck = d, phase = VPPhase.RoundEnd, resultLabel = label, credits = it.credits + pay, message = "結果：$label +$pay") }
    }

    fun nextRound() { _ui.update { VPState(credits = it.credits, bet = it.bet) } }

    // ---- 役判定（Jacks or Better）----
    private fun evaluateAndPayout(hand: List<Card>, bet: Int): Pair<String, Int> {
        val ranks = hand.map { it.rank }.sorted()
        val suits = hand.map { it.suit }
        val counts = ranks.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
        val isFlush = suits.toSet().size == 1
        val isStraight = isStraight(ranks) // A2345 / 10JQKA どちらも許可
        val label: String
        val mult: Int
        when {
            isFlush && isRoyal(ranks) -> { label = "Royal Flush"; mult = 250 }
            isFlush && isStraight     -> { label = "Straight Flush"; mult = 50 }
            counts.first().second == 4-> { label = "Four of a Kind"; mult = 25 }
            counts.first().second == 3 && counts.getOrNull(1)?.second == 2 -> { label = "Full House"; mult = 9 }
            isFlush -> { label = "Flush"; mult = 6 }
            isStraight -> { label = "Straight"; mult = 4 }
            counts.first().second == 3 -> { label = "Three of a Kind"; mult = 3 }
            counts.first().second == 2 && counts.getOrNull(1)?.second == 2 -> { label = "Two Pair"; mult = 2 }
            isJacksOrBetter(counts) -> { label = "Jacks or Better"; mult = 1 }
            else -> { label = "No Pair"; mult = 0 }
        }
        return label to (mult * bet)
    }

    private fun isStraight(ranks: List<Int>): Boolean {
        val r = ranks.sorted()
        // A(1)を14として評価も試す
        val rAceHigh = r.map { if (it==1) 14 else it }.sorted()
        fun seq(list: List<Int>) = list.zipWithNext().all { (a,b) -> b == a + 1 }
        return seq(r) || seq(rAceHigh)
    }
    private fun isRoyal(ranks: List<Int>): Boolean {
        val set = ranks.map { if (it==1) 14 else it }.toSet()
        return set == setOf(10,11,12,13,14)
    }
    private fun isJacksOrBetter(counts: List<Pair<Int,Int>>): Boolean {
        val pair = counts.firstOrNull { it.second == 2 }?.first ?: return false
        return pair in listOf(1,11,12,13) || pair >= 11 // A or J/Q/K
    }
}