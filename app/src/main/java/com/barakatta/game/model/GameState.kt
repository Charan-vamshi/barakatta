package com.barakatta.game.model

enum class TurnPhase { ROLLING, SELECTING, GAME_OVER }

/**
 * Complete mutable game state.  Mutated in-place by GameEngine; a new
 * reference is posted to LiveData after each state change so observers fire.
 */
data class GameState(
    val players: List<Player>,
    var currentPlayerIndex: Int = 0,
    var heldRolls: MutableList<Int> = mutableListOf(),
    var phase: TurnPhase = TurnPhase.ROLLING,
    var selectedRollIndex: Int = -1,
    var winner: Player? = null,
    var lastRoll: Int = 0,
    var statusMessage: String = ""
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]

    /** Advance to next non-winning player and reset turn state. */
    fun nextPlayer() {
        heldRolls.clear()
        selectedRollIndex = -1
        var next = (currentPlayerIndex + 1) % players.size
        var guard = 0
        while (players[next].isWinner && guard++ < players.size) {
            next = (next + 1) % players.size
        }
        currentPlayerIndex = next
        phase = TurnPhase.ROLLING
        statusMessage = "${currentPlayer.name}'s turn — Roll!"
    }
}
