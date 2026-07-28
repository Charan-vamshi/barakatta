package com.barakatta.game.model

// ── Game rules ────────────────────────────────────────────────────────────────

object GameRules {

    private val BONUS_SET = setOf(5, 6, 12)

    // Weighted RNG — 5, 6, 12 are rare; common values dominate
    // Total weight = 100
    private val ROLL_TABLE: IntArray = run {
        val weights = mapOf(1 to 20, 2 to 22, 3 to 22, 4 to 20, 5 to 7, 6 to 6, 12 to 3)
        val list = mutableListOf<Int>()
        for ((value, w) in weights) repeat(w) { list.add(value) }
        list.toIntArray()
    }

    /** Weighted random from {1,2,3,4,5,6,12}. */
    fun generateRoll(): Int = ROLL_TABLE.random()

    /** Checks if a roll grants another turn. */
    fun isBonus(roll: Int): Boolean = roll in BONUS_SET

    /**
     * Returns coin IDs that can legally use [roll] for [player].
     * Enforces: overshoot, ring gates (requires kill), friendly blocking.
     */
    fun getValidCoinsForRoll(
        roll: Int,
        player: Player,
        @Suppress("UNUSED_PARAMETER") allPlayers: List<Player>
    ): List<Int> = player.coins
        .filter { !it.isFinished && canCoinUseRoll(it, roll, player) }
        .map { it.id }

    private fun canCoinUseRoll(coin: Coin, roll: Int, player: Player): Boolean {
        val target = coin.pathIndex + roll
        if (target > 48) return false   // no overshoot

        // Ring-advancement gates (one kill unlocks both transitions)
        if (coin.pathIndex < 24 && target >= 24 && !player.hasKilled) return false
        if (coin.pathIndex in 24..39 && target >= 40 && !player.hasKilled) return false

        if (target == 48) return true   // reaching center always OK

        // Friendly-coin blocking on non-safe cell
        val targetCell = player.path[target]
        if (!Board.isSafeCell(targetCell)) {
            if (player.coins.any { o -> o.id != coin.id && !o.isFinished && o.cell(player.path) == targetCell })
                return false
        }
        return true
    }

    /**
     * Applies a validated move. Advances coin, handles finish and kill.
     * Returns [MoveResult]. Caller must have verified legality first.
     */
    fun applyMove(
        coin: Coin,
        roll: Int,
        player: Player,
        allPlayers: List<Player>
    ): MoveResult {
        coin.pathIndex += roll

        if (coin.pathIndex == 48) {
            coin.isFinished = true
            player.coinsFinished++
            return MoveResult(finished = true)
        }

        val targetCell = player.path[coin.pathIndex]
        if (!Board.isSafeCell(targetCell)) {
            for (opp in allPlayers) {
                if (opp.id == player.id) continue
                val killed = opp.coins.find { !it.isFinished && it.cell(opp.path) == targetCell }
                if (killed != null) {
                    killed.pathIndex = 0
                    player.hasKilled = true
                    return MoveResult(killed = true, killedCoin = killed, killedPlayer = opp)
                }
            }
        }
        return MoveResult()
    }
}
