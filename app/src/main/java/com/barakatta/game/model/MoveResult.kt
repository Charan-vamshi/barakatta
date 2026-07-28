package com.barakatta.game.model

// ── Move result ───────────────────────────────────────────────────────────────

data class MoveResult(
    val finished: Boolean = false,
    val killed: Boolean = false,
    val killedCoin: Coin? = null,
    val killedPlayer: Player? = null
)

// ── UI Animation Data ─────────────────────────────────────────────────────────

data class CoinAnimData(
    val playerId: Int,
    val coinId: Int,
    val pathCells: List<Cell>,
    val isKill: Boolean,
    val isFinish: Boolean
)
