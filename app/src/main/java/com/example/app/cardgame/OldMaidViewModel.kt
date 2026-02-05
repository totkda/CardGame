package com.example.app.cardgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class OldMaidPhase { BotThinking, WaitingForHumanPick, Resolving, GameOver }

data class OldMaidUiState(
    val players: List<PlayerSummary> = emptyList(),
    val meIndex: Int = 0,
    val current: Int = 0,
    val leftNeighborOfCurrent: Int = 0,
    val myHand: List<Card> = emptyList(),
    val neighborBackCount: Int = 0,
    val lastDrawn: Card? = null,
    val message: String = "",
    val phase: OldMaidPhase = OldMaidPhase.WaitingForHumanPick,
    val orderOut: List<Int> = emptyList() // 上がり順（id）
)

data class PlayerSummary(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val count: Int,
    val finished: Boolean
)

class OldMaidViewModel : ViewModel() {

    // ---- 内部ゲーム状態 ----
    private var players = mutableListOf<Player>()
    private var currentIndex = 0
    private val orderOut = mutableListOf<Int>() // 上がった順

    private val _ui = MutableStateFlow(OldMaidUiState())
    val ui: StateFlow<OldMaidUiState> = _ui

    fun start(numPlayers: Int = 3, mePos: Int = 0) {
        require(numPlayers in 2..4)
        players = MutableList(numPlayers) { i ->
            Player(
                id = i,
                name = if (i == mePos) "あなた" else "CPU${i + 1}",
                isHuman = (i == mePos)
            )
        }
        orderOut.clear()
        currentIndex = 0

        // デッキ作成→シャッフル→配札（ラウンドロビン）
        val deck = fullDeckWithJoker()
        deck.shuffle(Random(System.currentTimeMillis()))
        var idx = 0
        while (deck.isNotEmpty()) {
            val card = deck.removeLast()
            players[idx].hand += card
            idx = (idx + 1) % players.size
        }

        // 初期ペアの自動捨て
        players.forEach { p ->
            p.hand.shuffle() // 引かれる側の順序性を隠す
            removePairsAll(p.hand)
            if (p.hand.isEmpty()) {
                p.finished = true
                orderOut += p.id
            }
        }

        // 最初の手番を、生きているプレイヤーに合わせる
        if (players[currentIndex].finished) {
            currentIndex = nextAlive(currentIndex)
        }
        pushUi(message = "ゲーム開始。左隣から1枚引きます。", lastDrawn = null)

        // ヒトの手番/CPU手番に応じてフェーズ設定
        proceedTurn()
    }

    /** 現在の手番者の左隣 index（alive をスキップ） */
    private fun leftNeighbor(i: Int): Int {
        var j = (i + 1) % players.size // 「左」=インデックス+1方向
        while (players[j].finished || players[j].hand.isEmpty()) {
            j = (j + 1) % players.size
            if (j == i) break // 安全弁
        }
        return j
    }

    /** 次の alive 手番者 */
    private fun nextAlive(i: Int): Int {
        var j = (i + 1) % players.size
        while (players[j].finished || players[j].hand.isEmpty()) {
            j = (j + 1) % players.size
            if (j == i) break
        }
        return j
    }

    /** UI反映 */
    private fun pushUi(message: String, lastDrawn: Card?) {
        val meIdx = players.indexOfFirst { it.isHuman }.coerceAtLeast(0)
        val left = leftNeighbor(currentIndex)
        _ui.value = OldMaidUiState(
            players = players.map {
                PlayerSummary(
                    id = it.id, name = it.name, isHuman = it.isHuman,
                    count = it.hand.size, finished = it.finished
                )
            },
            meIndex = meIdx,
            current = currentIndex,
            leftNeighborOfCurrent = left,
            myHand = players[meIdx].hand.sortedWith(compareBy({ it.rank == 0 }, { it.rank })), // Joker最後尾
            neighborBackCount = players[left].hand.size,
            lastDrawn = lastDrawn,
            message = message,
            phase = when {
                isGameOver() -> OldMaidPhase.GameOver
                players[currentIndex].isHuman -> OldMaidPhase.WaitingForHumanPick
                else -> OldMaidPhase.BotThinking
            },
            orderOut = orderOut.toList()
        )
    }

    /** ゲーム終了？（残り1名のみ未上がり=敗者確定） */
    private fun isGameOver(): Boolean {
        val alive = players.count { !it.finished && it.hand.isNotEmpty() }
        return alive <= 1
    }

    private fun finishIfNeeded(playerIdx: Int) {
        val p = players[playerIdx]
        if (!p.finished && p.hand.isEmpty()) {
            p.finished = true
            orderOut += p.id
        }
    }

    /** 共通：left から index 位置の1枚を引く（index は 0..left手札-1） */
    private fun drawFromLeft(current: Int, pickIndex: Int): Card {
        val left = leftNeighbor(current)
        val leftHand = players[left].hand
        val realIndex = pickIndex.coerceIn(0, leftHand.lastIndex)
        val card = leftHand.removeAt(realIndex)
        players[current].hand += card
        return card
    }

    /** 共通：引いた後のペア除去と、上がり/ゲーム終了判定→次の手番へ */
    private fun resolveAfterDraw(current: Int, drawn: Card) {
        removePairsAll(players[current].hand)
        finishIfNeeded(current)
        finishIfNeeded(leftNeighbor(current)) // 引かれた側もゼロになった可能性

        if (isGameOver()) {
            // 最後に残った（未finishedの）id = 敗者
            val loserId = players.firstOrNull { !it.finished && it.hand.isNotEmpty() }?.id
            val msg = if (loserId != null) {
                val name = players.first { it.id == loserId }.name
                "ゲーム終了。敗者は $name（JOKER保持者）"
            } else "ゲーム終了。"
            pushUi(message = msg, lastDrawn = drawn)
            return
        }

        // 次の alive 手番へ
        currentIndex = nextAlive(currentIndex)
        pushUi(message = "${players[currentIndex].name} の番です。左隣から1枚引きます。", lastDrawn = drawn)
        proceedTurn()
    }

    /** 手番進行（CPUなら自動、ヒトなら待機） */
    private fun proceedTurn() {
        val cur = currentIndex
        if (players[cur].isHuman) {
            // UIでクリック待ち
            pushUi(message = "左隣の裏カードから1枚選んでください。", lastDrawn = null)
            return
        }
        // CPUターン：少し待ってからランダムに1枚引く
        viewModelScope.launch {
            _ui.update { it.copy(phase = OldMaidPhase.BotThinking) }
            delay(700)
            val left = leftNeighbor(cur)
            val pick = if (players[left].hand.isEmpty()) 0 else Random.nextInt(players[left].hand.size)
            val drawn = drawFromLeft(cur, pick)
            delay(400)
            resolveAfterDraw(cur, drawn)
        }
    }

    /** ヒトの入力：左隣の index をタップした */
    fun onHumanPick(index: Int) {
        val cur = currentIndex
        if (!players[cur].isHuman) return
        val left = leftNeighbor(cur)
        if (players[left].hand.isEmpty()) return
        _ui.update { it.copy(phase = OldMaidPhase.Resolving) }

        viewModelScope.launch {
            val drawn = drawFromLeft(cur, index)
            delay(300)
            resolveAfterDraw(cur, drawn)
        }
    }
}
