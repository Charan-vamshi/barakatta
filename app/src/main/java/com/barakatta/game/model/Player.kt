package com.barakatta.game.model

/**
 * Represents one player in the game.
 * [path] is the prebuilt 49-cell ordered list from Board.buildPlayerPath().
 */
data class Player(
    val id: Int,
    val name: String,
    val color: Int,               // ARGB packed int
    val homeCell: Cell,
    val path: List<Cell>,
    val coins: MutableList<Coin> = mutableListOf(),
    var hasKilled: Boolean = false,
    var coinsFinished: Int = 0
) {
    /** True when all 6 coins have reached center D4. */
    val isWinner: Boolean get() = coinsFinished >= 6

    fun coin(id: Int): Coin = coins.first { it.id == id }
}
