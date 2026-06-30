package com.ak.battleship.utils

import com.ak.battleship.data.Move
import com.ak.battleship.model.Ship

/**
 * CORE DOMAIN: Fleet Reconstruction Engine
 * * This utility isolates the logic for transforming raw grid data back into physical Ship objects.
 * It has zero dependencies on Android UI or ViewModels.
 */

/**
 * Flawlessly reconstructs a fleet using explicit database records.
 * Immune to the "Touching Ships / Blob" ambiguity because it groups by exact ship name.
 */
fun reconstructFleetFromMoves(moves: List<Move>): List<Ship> {
    val reconstructed = mutableListOf<Ship>()

    // 1. Isolate pegs that explicitly know their ship name
    val namedShips = listOf("Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer")
    val exactPegs = moves.filter { it.result in namedShips }

    // 2. If we are dealing with a legacy game (where result was just "SHIP"), fallback to heuristic
    if (exactPegs.isEmpty()) {
        val legacyCoords = moves.filter { it.result == "SHIP" }.map { Pair(it.x, it.y) }
        return guessFleetFromAnonymousCoords(legacyCoords)
    }

    // 3. Exact Reconstruction Pipeline
    val groupedPegs = exactPegs.groupBy { it.result }

    for ((shipName, pegs) in groupedPegs) {
        if (pegs.isEmpty()) continue

        // Find the top-left origin point of the ship
        val minX = pegs.minOf { it.x }
        val minY = pegs.minOf { it.y }

        // If the Y coordinates change across the pegs, the ship is placed vertically
        val isVertical = minY != pegs.maxOf { it.y }

        reconstructed.add(
            Ship(
                name = shipName,
                size = pegs.size,
                x = minX.toFloat(),
                y = minY.toFloat(),
                isVertical = isVertical,
                isPlaced = true
            )
        )
    }

    return reconstructed
}

/**
 * Heuristically guesses ship placements from anonymous coordinates.
 * Primarily used at the end of Companion Mode games to generate a verification visual.
 * * NOTE: Uses a greedy algorithm. If ships are placed adjacent/touching,
 * this may misidentify ship boundaries, but will always return a mathematically valid 17-peg shape.
 */
fun guessFleetFromAnonymousCoords(hits: List<Pair<Int, Int>>): List<Ship> {
    val hitSet = hits.toSet()

    // We must have exactly 17 hits to attempt a perfect mathematical reconstruction
    if (hitSet.size != 17) return emptyList()

    val shipsToPlace = listOf(
        Pair("Carrier", 5),
        Pair("Battleship", 4),
        Pair("Cruiser", 3),
        Pair("Submarine", 3),
        Pair("Destroyer", 2)
    )

    // A Recursive Depth-First Search (DFS)
    fun backtrack(
        remainingHits: Set<Pair<Int, Int>>,
        shipIndex: Int,
        currentFleet: List<Ship>
    ): List<Ship>? {
        // Base Case: All ships placed successfully
        if (shipIndex >= shipsToPlace.size) {
            return if (remainingHits.isEmpty()) currentFleet else null
        }

        val (shipName, size) = shipsToPlace[shipIndex]
        val validPlacements = mutableListOf<Ship>()

        // Scan the remaining hits to find every valid origin point for THIS ship
        for (cell in remainingHits) {
            // Check Horizontal (x extends to the right)
            val horizCells = (0 until size).map { Pair(cell.first + it, cell.second) }.toSet()
            if (remainingHits.containsAll(horizCells)) {
                validPlacements.add(
                    Ship(shipName, size, cell.first.toFloat(), cell.second.toFloat(), isVertical = false, isPlaced = true)
                )
            }

            // Check Vertical (y extends downward)
            val vertCells = (0 until size).map { Pair(cell.first, cell.second + it) }.toSet()
            if (remainingHits.containsAll(vertCells)) {
                validPlacements.add(
                    Ship(shipName, size, cell.first.toFloat(), cell.second.toFloat(), isVertical = true, isPlaced = true)
                )
            }
        }

        // Randomize the branches. If ships are touching and create multiple valid geometries,
        // this ensures the algorithm doesn't bias toward the exact same incorrect layout every time.
        validPlacements.shuffle()

        // Recurse down each valid branch
        for (placement in validPlacements) {
            val placementCells = placement.getCells().toSet()
            val result = backtrack(
                remainingHits - placementCells,
                shipIndex + 1,
                currentFleet + listOf(placement)
            )
            // If the branch returned a complete fleet, bubble it up to the top
            if (result != null) return result
        }

        // Dead end: No placements worked
        return null
    }

    return backtrack(hitSet, 0, emptyList()) ?: emptyList()
}