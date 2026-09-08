package com.ak.battleship.model

import kotlin.math.roundToInt

/**
 * CORE DOMAIN: Ship Entity
 * * Represents a physical ship on the Battleship grid.
 * * Designed to be STRICTLY IMMUTABLE (`val`). All UI state changes must generate a new
 * object using the `.copy()` function to ensure Jetpack Compose recomposes correctly.
 * * @property name The classification of the ship (e.g., "Carrier").
 * @property size The exact peg length of the ship (2 to 5).
 * @property x The horizontal position (Float required for smooth Compose dragging).
 * @property y The vertical position (Float required for smooth Compose dragging).
 * @property isVertical True if pointing downwards, False if pointing right.
 * @property isPlaced True if firmly dropped onto the grid, False if in the staging area.
 */
data class Ship(
    val name: String,
    val size: Int,
    val x: Float,
    val y: Float,
    val isVertical: Boolean = false,
    val isPlaced: Boolean = false
) {
    /**
     * Mathematically projects the ship's smooth floating origin (x,y) into exact integer grid coordinates.
     * Safely ignores unplaced ships to prevent accidental out-of-bounds hits.
     */
    fun getCells(): List<Pair<Int, Int>> {
        if (!isPlaced) return emptyList()

        // Snap the floating UI coordinates to the strict mathematical grid
        val startX = x.roundToInt()
        val startY = y.roundToInt()

        return (0 until size).map { i ->
            if (isVertical) Pair(startX, startY + i) else Pair(startX + i, startY)
        }
    }
}

fun isShipData(result: String): Boolean {
    return result == "SHIP" || result in listOf("Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer")
}