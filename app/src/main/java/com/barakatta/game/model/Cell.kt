package com.barakatta.game.model

/**
 * A single cell on the 7×7 board.
 *
 * Physical board orientation (as the user draws it):
 *   rowIndex : 0 = A (top row)  …  6 = G (bottom row)
 *   colIndex : 1 (left column)  …  7 (right column)
 *
 * Notable landmarks:
 *   A1 = top-left,  A7 = top-right
 *   G1 = bottom-left, G7 = bottom-right
 *   D4 = center (finish)
 */
data class Cell(val rowIndex: Int, val colIndex: Int) {
    val rowChar: Char get() = 'A' + rowIndex
    val label: String get() = "${rowChar}${colIndex}"
    override fun toString(): String = label
}
