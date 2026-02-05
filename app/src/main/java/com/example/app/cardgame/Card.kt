// app/src/main/java/com/example/app/cardgame/Card.kt
package com.example.app.cardgame

enum class Suit { CLUBS, DIAMONDS, HEARTS, SPADES, JOKER } // ★ JOKERを含める

data class Card(val suit: Suit, val rank: Int) // rank: 1(A)..13(K), JOKER=0