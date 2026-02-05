package com.example.app.cardgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HighLowState(
    val current: Card? = null,     // 左：常に表
    val next: Card? = null,        // 右：新しく引いたカード
    val nextFaceUp: Boolean = false, // 右カードの表裏（false=裏）
    val remaining: Int = 52,
    val score: Int = 10,           // 初期値10
    val message: String = "Up か Down を選んでね",
    val gameOver: Boolean = false,
    val flipNextKey: Int = 0       // フリップ用トリガ（UIアニメ再生に使う）
)

class HighLowViewModel : ViewModel() {
    private var deck = Deck(seed = System.currentTimeMillis())
    private val _ui = MutableStateFlow(HighLowState())
    val ui: StateFlow<HighLowState> = _ui

    init { start() }

    fun start() {
        deck = Deck(seed = System.currentTimeMillis())
        val first = deck.draw()
        val second = deck.draw()
        _ui.value = HighLowState(
            current = first,
            next = second,
            nextFaceUp = false,           // 右は裏から開始
            remaining = deck.size(),
            score = 10,                   // スコア初期化
            message = "次は Up か Down？",
            gameOver = false,
            flipNextKey = 0
        )
    }

    // ポーカーの強さ順：A(1)を最強として14に変換。それ以外はそのまま。
    private fun rankWeight(r: Int): Int = when (r) {
        1 -> 14            // A
        else -> r          // 2..13 (J=11,Q=12,K=13)
    }

    fun guess(up: Boolean) {
        val curr = _ui.value.current ?: return
        val nxt = _ui.value.next ?: return
        if (_ui.value.gameOver) return

        // 判定：A最強 / 2最弱 で比較
        val cmp = rankWeight(nxt.rank).compareTo(rankWeight(curr.rank))
        val correct = if (cmp == 0) {
            false // 同値は不正解扱い（仕様に合わせる）
        } else {
            if (up) (cmp > 0) else (cmp < 0)
        }

        // まずは右カードを表にして見せる（フリップ）
        _ui.update { prev ->
            val newScore = if (correct) prev.score * 2 else 0
            val over = !correct
            prev.copy(
                nextFaceUp = true,
                score = newScore,
                message = if (correct) "正解！ スコアは ${newScore}" else "はずれ… ゲーム終了 (スコア0)",
                gameOver = over,
                flipNextKey = prev.flipNextKey + 1
            )
        }

        // 不正解なら終了、正解なら少し見せてから次のラウンドへ
        if (!correct) return

        viewModelScope.launch {
            delay(600) // 表で見せる時間
            val newNext = deck.draw()
            _ui.update { prev ->
                prev.copy(
                    current = prev.next,          // 右を左へスライドするイメージで状態更新
                    next = newNext,               // 新しい右カードを用意
                    nextFaceUp = false,           // 右は裏から
                    remaining = deck.size(),
                    message = if (deck.size() == 0) "山札が空です" else "次は Up か Down？",
                    // 次のフリップはまだしないので flipNextKey は据え置き
                    gameOver = deck.size() == 0   // 山札が尽きたら終了
                )
            }
        }
    }
}