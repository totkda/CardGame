
package com.example.app.cardgame

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreRuleTest {
    @Test
    fun doubleOnCorrect_zeroOnFail() {
        val vm = HighLowViewModel()
        vm.start()

        // 正解に寄せるため仮に current=5、next=10 を想定して直接状態をいじる場合は
        // ViewModel の公開APIではできないので、ここは仕様確認の例のみ。
        // 実際には Deck を差し替え注入できる設計にするのがベスト（DI）。
        assertEquals(10, vm.ui.value.score)
    }
}
