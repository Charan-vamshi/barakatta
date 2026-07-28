package com.barakatta.game.model

/**
 * Board constants and per-player 49-cell path builder.
 *
 * Coordinate system  : rowIndex 0=A(top)…6=G(bottom), colIndex 1(left)…7(right)
 * Center finish cell : D4 = Cell(3, 4)
 *
 * Path layout (49 cells, indices 0–48):
 *   0–23  : outer ring  (anti-clockwise, starts at home cell)
 *   24–39 : middle ring (clockwise,      starts at player's middle-ring entry)
 *   40–47 : inner ring  (clockwise,      starts at player's inner-ring entry)
 *   48    : center D4
 *
 * Ring directions (user-confirmed):
 *   Outer  → anti-clockwise
 *   Middle → clockwise
 *   Inner  → clockwise
 *
 * Entry points (user-confirmed for G4; mirrored for others):
 *   G4: outer ends G3 → middle entry F2 → middle ends F3 → inner entry E3 → inner ends E4 → D4
 *   A4: outer ends A5 → middle entry B6 → middle ends B5 → inner entry C5 → inner ends C4 → D4
 *   D7: outer ends E7 → middle entry F6 → middle ends E6 → inner entry E5 → inner ends D5 → D4
 *   D1: outer ends C1 → middle entry B2 → middle ends C2 → inner entry C3 → inner ends D3 → D4
 */
object Board {

    val CENTER = Cell(3, 4)   // D4

    // ── Safe cells ────────────────────────────────────────────────────────────
    val OUTER_SAFE_CELLS: Set<Cell> = setOf(
        Cell(0, 1), Cell(0, 4), Cell(0, 7),   // A1, A4, A7
        Cell(3, 1), Cell(3, 7),                // D1, D7
        Cell(6, 1), Cell(6, 4), Cell(6, 7)    // G1, G4, G7
    )
    val MIDDLE_SAFE_CELLS: Set<Cell> = setOf(
        Cell(1, 2), Cell(1, 6),               // B2, B6
        Cell(5, 2), Cell(5, 6)                // F2, F6
    )
    val SAFE_CELLS: Set<Cell> = OUTER_SAFE_CELLS + MIDDLE_SAFE_CELLS

    // ── Home cells by player count ────────────────────────────────────────────
    val HOME_CELLS_BY_COUNT: Map<Int, List<Cell>> = mapOf(
        2 to listOf(Cell(6, 4), Cell(0, 4)),
        3 to listOf(Cell(6, 4), Cell(0, 4), Cell(3, 7)),
        4 to listOf(Cell(6, 4), Cell(0, 4), Cell(3, 7), Cell(3, 1))
    )

    // ── Outer ring base (anti-clockwise, 24 cells, starting A7) ──────────────
    // Top right→left: A7…A1  (7)
    // Left top→bottom: B1…G1  (6)
    // Bottom left→right: G2…G7  (6)
    // Right bottom→top: F7…B7  (5)
    private val OUTER_RING_BASE: List<Cell> = buildList {
        for (col in 7 downTo 1) add(Cell(0, col))    // A7→A1
        for (row in 1..6)       add(Cell(row, 1))     // B1→G1
        for (col in 2..7)       add(Cell(6, col))     // G2→G7
        for (row in 5 downTo 1) add(Cell(row, 7))     // F7→B7
    }  // total = 7+6+6+5 = 24 ✓

    // ── Middle ring base (clockwise, 16 cells, starting B2) ──────────────────
    // Top left→right: B2…B6  (5)
    // Right top→bottom: C6…F6  (4)
    // Bottom right→left: F5…F2  (4)
    // Left bottom→top: E2…C2  (3)
    private val MIDDLE_RING_BASE: List<Cell> = buildList {
        for (col in 2..6)       add(Cell(1, col))     // B2→B6
        for (row in 2..5)       add(Cell(row, 6))     // C6→F6
        for (col in 5 downTo 2) add(Cell(5, col))     // F5→F2
        for (row in 4 downTo 2) add(Cell(row, 2))     // E2→C2
    }  // total = 5+4+4+3 = 16 ✓

    // ── Inner ring base (clockwise, 8 cells, starting C3) ────────────────────
    // Top left→right: C3…C5  (3)
    // Right top→bottom: D5…E5  (2)
    // Bottom right→left: E4…E3  (2)
    // Left bottom→top: D3  (1)
    private val INNER_RING_BASE: List<Cell> = buildList {
        for (col in 3..5)       add(Cell(2, col))     // C3→C5
        for (row in 3..4)       add(Cell(row, 5))     // D5→E5
        for (col in 4 downTo 3) add(Cell(4, col))     // E4→E3
        add(Cell(3, 3))                                // D3
    }  // total = 3+2+2+1 = 8 ✓

    // ── Ring entry points (user-confirmed, mirrored) ──────────────────────────
    private val MIDDLE_ENTRY: Map<Cell, Cell> = mapOf(
        Cell(6, 4) to Cell(5, 2),   // G4 → F2
        Cell(0, 4) to Cell(1, 6),   // A4 → B6
        Cell(3, 7) to Cell(5, 6),   // D7 → F6
        Cell(3, 1) to Cell(1, 2)    // D1 → B2
    )
    private val INNER_ENTRY: Map<Cell, Cell> = mapOf(
        Cell(6, 4) to Cell(4, 3),   // G4 → E3
        Cell(0, 4) to Cell(2, 5),   // A4 → C5
        Cell(3, 7) to Cell(4, 5),   // D7 → E5
        Cell(3, 1) to Cell(2, 3)    // D1 → C3
    )

    // ── Public API ────────────────────────────────────────────────────────────

    fun isSafeCell(cell: Cell): Boolean = cell in SAFE_CELLS

    /** 0=outer, 1=middle, 2=inner, 3=center */
    fun getRing(cell: Cell): Int = when {
        cell == CENTER -> 3
        cell.rowIndex == 0 || cell.rowIndex == 6 ||
                cell.colIndex == 1 || cell.colIndex == 7 -> 0
        cell.rowIndex == 1 || cell.rowIndex == 5 ||
                cell.colIndex == 2 || cell.colIndex == 6 -> 1
        cell.rowIndex == 2 || cell.rowIndex == 4 ||
                cell.colIndex == 3 || cell.colIndex == 5 -> 2
        else -> 3
    }

    /**
     * Builds the complete 49-cell path for a player starting at [homeCell].
     * index 0 = homeCell (outer ring start), index 48 = D4 (center / finish).
     */
    fun buildPlayerPath(homeCell: Cell): List<Cell> = buildList {
        addAll(rotateToStart(OUTER_RING_BASE, homeCell))                          // 0–23
        val midEntry = requireNotNull(MIDDLE_ENTRY[homeCell]) { "No mid entry for $homeCell" }
        addAll(rotateToStart(MIDDLE_RING_BASE, midEntry))                         // 24–39
        val innerEntry = requireNotNull(INNER_ENTRY[homeCell]) { "No inner entry for $homeCell" }
        addAll(rotateToStart(INNER_RING_BASE, innerEntry))                        // 40–47
        add(CENTER)                                                                // 48
    }

    private fun rotateToStart(ring: List<Cell>, start: Cell): List<Cell> {
        val idx = ring.indexOf(start)
        require(idx >= 0) { "Cell $start not in ring" }
        return ring.subList(idx, ring.size) + ring.subList(0, idx)
    }
}
