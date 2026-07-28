package com.barakatta.game.model

/**
 * A single player's coin. pathIndex locates it in the player's 49-cell path:
 *   0        = home cell (outer ring position 0)
 *   1–23     = outer ring
 *   24–39    = middle ring
 *   40–47    = inner ring
 *   48       = center D4 (finished)
 */
data class Coin(
    val id: Int,           // 0–5
    val playerId: Int,
    var pathIndex: Int = 0,
    var isFinished: Boolean = false
) {
    val isAtHome: Boolean get() = pathIndex == 0

    /** Returns the board Cell this coin occupies. */
    fun cell(path: List<Cell>): Cell = path[pathIndex.coerceIn(0, path.size - 1)]
}
