// app/src/main/java/com/example/app/cardgame/AppRoot.kt
package com.example.app.cardgame

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*

enum class AppScreen { Menu, UpDown, OldMaid, Daifugo, Blackjack, VideoPoker }


@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.Menu) }
    MaterialTheme {
        Surface {
            when (screen) {
                AppScreen.Menu -> GameSelectionScreen(
                    onGoUpDown   = { screen = AppScreen.UpDown },
                    onGoOldMaid  = { screen = AppScreen.OldMaid },
                    onGoDaifugo  = { screen = AppScreen.Daifugo },
                    onGoBlackjack= { screen = AppScreen.Blackjack },
                    onGoVideoPoker = { screen = AppScreen.VideoPoker }
                )
                AppScreen.UpDown   -> HighLowScreen(onBack = { screen = AppScreen.Menu })
                AppScreen.OldMaid  -> OldMaidScreen(onBack = { screen = AppScreen.Menu })
                AppScreen.Daifugo  -> DaifugoScreen(onBack = { screen = AppScreen.Menu })
                AppScreen.Blackjack-> BlackjackScreen(onBack = { screen = AppScreen.Menu })
                AppScreen.VideoPoker -> VideoPokerScreen(onBack = { screen = AppScreen.Menu })
            }
        }
    }
}
