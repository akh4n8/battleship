package com.ak.battleship.ai

import org.junit.Assert.*
import org.junit.Test

/**
 * UNIT TESTS: The Deduction Engine
 * Validates the core geometric and mathematical rules of the AI.
 */
class DeductionEngineTest {

    // Helper function to generate an empty 10x10 ocean
    private fun getEmptyBoard(): Array<IntArray> {
        return Array(10) { IntArray(10) { DeductionEngine.CELL_UNKNOWN } }
    }

    @Test
    fun `executeLinearKill - Horizontal Open Ocean`() {
        // 1. SETUP: Create a board with a horizontal line of 3 hits
        val board = getEmptyBoard()
        val hits = listOf(Pair(4, 5), Pair(5, 5), Pair(6, 5))

        // Inject hits into the board
        for ((x, y) in hits) {
            board[x][y] = DeductionEngine.CELL_HIT
        }

        // 2. EXECUTE: Ask the engine where to shoot next
        val target = DeductionEngine.executeLinearKill(board, hits, emptySet())

        // 3. ASSERT: It MUST target either the left end (3, 5) or right end (7, 5)
        assertNotNull("Target should not be null", target)
        val isValidTarget = target == Pair(3, 5) || target == Pair(7, 5)
        assertTrue("Target $target is not a valid horizontal endpoint", isValidTarget)
    }

    @Test
    fun `executeLinearKill - Wall Constraint forces specific target`() {
        // 1. SETUP: Ship is jammed against the top wall (y=0)
        val board = getEmptyBoard()
        val hits = listOf(Pair(3, 0), Pair(3, 1))

        for ((x, y) in hits) {
            board[x][y] = DeductionEngine.CELL_HIT
        }

        // 2. EXECUTE
        val target = DeductionEngine.executeLinearKill(board, hits, emptySet())

        // 3. ASSERT: It cannot shoot up (-1). It MUST shoot down at (3, 2)
        assertEquals("Failed to respect wall boundary", Pair(3, 2), target)
    }

    @Test
    fun `executeLinearKill - Miss Constraint forces specific target`() {
        // 1. SETUP: Ship is blocked by a previous MISS on the right
        val board = getEmptyBoard()
        val hits = listOf(Pair(5, 5), Pair(6, 5))

        for ((x, y) in hits) { board[x][y] = DeductionEngine.CELL_HIT }
        board[7][5] = DeductionEngine.CELL_MISS // Block the right side

        // 2. EXECUTE
        val target = DeductionEngine.executeLinearKill(board, hits, emptySet())

        // 3. ASSERT: It cannot shoot right (7,5). It MUST shoot left at (4, 5)
        assertEquals("Failed to respect previous miss", Pair(4, 5), target)
    }

    @Test
    fun `executeLinearKill - Fused Ship Hallucination Prevention`() {
        // 1. SETUP: An active ship is touching a dead, SUNK ship
        val board = getEmptyBoard()

        // The dead submarine (SUNK)
        board[2][2] = DeductionEngine.CELL_SUNK
        board[3][2] = DeductionEngine.CELL_SUNK
        board[4][2] = DeductionEngine.CELL_SUNK
        val claimedHits = setOf(Pair(2, 2), Pair(3, 2), Pair(4, 2))

        // The active hits touching it
        board[5][2] = DeductionEngine.CELL_HIT
        board[6][2] = DeductionEngine.CELL_HIT
        val activeHits = listOf(Pair(2, 2), Pair(3, 2), Pair(4, 2), Pair(5, 2), Pair(6, 2))

        // 2. EXECUTE
        val target = DeductionEngine.executeLinearKill(board, activeHits, claimedHits)

        // 3. ASSERT: It must NOT jump over the SUNK ship to shoot at (1,2).
        // It must realize the left side is dead and strictly target the right side (7,2)
        assertEquals("Failed to respect SUNK corpse boundaries", Pair(7, 2), target)
    }
}