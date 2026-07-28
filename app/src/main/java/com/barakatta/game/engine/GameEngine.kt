package com.barakatta.game.engine

import com.barakatta.game.model.*

/**
 * Turn state machine.
 *
 * Roll flow:
 *  • [performRoll] generates one value; if bonus (1,5,6,12) and bonusCount < 3,
 *    caller should invoke it again after showing the animation.
 *  • All rolled values accumulate in state.heldRolls.
 *  • [selectRoll] → [applySelectedRoll] consume rolls one at a time.
 *  • A kill appends another roll chain via [appendKillBonus].
 */
class GameEngine(playerCount: Int, playerNames: List<String>) {

    val state: GameState

    private val playerColors = listOf(
        0xFFF44336.toInt(),   // Red   – Player 1
        0xFF2196F3.toInt(),   // Blue  – Player 2
        0xFF4CAF50.toInt(),   // Green – Player 3
        0xFFFFC107.toInt()    // Amber – Player 4
    )

    /** Running count of consecutive bonus rolls in the current chain. */
    var currentBonusCount = 0
        private set

    init {
        val homes = requireNotNull(Board.HOME_CELLS_BY_COUNT[playerCount]) {
            "Invalid player count: $playerCount"
        }
        val players = homes.mapIndexed { i, home ->
            val path = Board.buildPlayerPath(home)
            val name = playerNames.getOrElse(i) { "Player ${i + 1}" }
            Player(id = i, name = name, color = playerColors[i], homeCell = home, path = path).also { p ->
                repeat(6) { j -> p.coins.add(Coin(id = j, playerId = i)) }
            }
        }
        state = GameState(players = players, statusMessage = "${players[0].name}'s turn — Roll!")
    }

    // ── Roll (one value at a time) ────────────────────────────────────────────

    /**
     * Generates and records ONE roll value. Updates [currentBonusCount].
     * @return the rolled value.
     */
    fun performRoll(): Int {
        val v = GameRules.generateRoll()
        state.heldRolls.add(v)
        state.lastRoll = v
        if (GameRules.isBonus(v)) currentBonusCount++ else currentBonusCount = 0
        return v
    }

    /** Call after the bonus chain ends (non-bonus rolled or cap hit). */
    fun finishRolling() {
        currentBonusCount = 0
        state.phase = TurnPhase.SELECTING
        autoSelectFirstRoll()
        state.statusMessage = buildMsg()
    }
    
    /** Automatically selects the first valid roll, or index 0 if none. */
    fun autoSelectFirstRoll() {
        if (state.heldRolls.isEmpty()) return
        val validIndex = state.heldRolls.indexOfFirst { roll ->
            GameRules.getValidCoinsForRoll(roll, state.currentPlayer, state.players).isNotEmpty()
        }
        state.selectedRollIndex = if (validIndex != -1) validIndex else 0
    }

    /** Whether the bonus chain should continue. */
    fun shouldContinueChain(lastRoll: Int): Boolean =
        GameRules.isBonus(lastRoll)

    // ── Select & apply ────────────────────────────────────────────────────────

    fun selectRoll(index: Int) {
        require(index in state.heldRolls.indices)
        state.selectedRollIndex = index
        state.statusMessage = "${state.currentPlayer.name}: Move a coin with ${state.heldRolls[index]}"
    }

    /**
     * Moves coin [coinId] using the selected roll.
     * Does NOT append kill bonus (ViewModel drives the bonus roll chain after animation).
     */
    fun applySelectedRoll(coinId: Int): MoveResult {
        val ri   = state.selectedRollIndex
        require(ri in state.heldRolls.indices)
        val roll = state.heldRolls[ri]
        val player = state.currentPlayer
        val coin   = player.coin(coinId)

        val result = GameRules.applyMove(coin, roll, player, state.players)
        state.heldRolls.removeAt(ri)
        state.selectedRollIndex = -1

        if (player.isWinner) {
            state.winner = player
            state.phase  = TurnPhase.GAME_OVER
            state.statusMessage = "🎉 ${player.name} wins!"
            return result
        }
        return result
    }

    /** Appends kill-bonus roll(s) to heldRolls (same chain rules apply). */
    fun appendKillBonus(): Int {
        val v = GameRules.generateRoll()
        state.heldRolls.add(v)
        state.lastRoll = v
        if (GameRules.isBonus(v)) currentBonusCount++ else currentBonusCount = 0
        return v
    }

    /** Call once all kill-bonus rolls are done. */
    fun finishKillBonus() {
        currentBonusCount = 0
        state.phase = TurnPhase.SELECTING
        autoSelectFirstRoll()
        state.statusMessage = buildMsg()
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    fun getValidCoinsForSelectedRoll(): List<Int> {
        val ri = state.selectedRollIndex
        if (ri !in state.heldRolls.indices) return emptyList()
        return GameRules.getValidCoinsForRoll(state.heldRolls[ri], state.currentPlayer, state.players)
    }

    fun hasAnyValidMoves(): Boolean = state.heldRolls.any { roll ->
        GameRules.getValidCoinsForRoll(roll, state.currentPlayer, state.players).isNotEmpty()
    }

    fun forceEndTurn() {
        state.heldRolls.clear()
        state.nextPlayer()
    }

    fun endTurnIfEmpty() {
        if (state.heldRolls.isEmpty()) {
            state.nextPlayer()
        } else {
            state.phase = TurnPhase.SELECTING
            state.statusMessage = buildMsg()
        }
    }

    private fun buildMsg(): String {
        val p = state.currentPlayer
        return when (state.phase) {
            TurnPhase.ROLLING   -> "${p.name}'s turn — Roll!"
            TurnPhase.SELECTING -> "${p.name} — rolls: [${state.heldRolls.joinToString(", ")}]"
            TurnPhase.GAME_OVER -> "🎉 ${state.winner?.name} wins!"
        }
    }
}
