package com.ak.battleship.ai

import android.content.Context
import com.ak.battleship.data.Move
import com.ak.battleship.model.BotDecision
import com.ak.battleship.model.Ship
import com.ak.battleship.model.isShipData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.random.Random
import androidx.annotation.VisibleForTesting

/**
 * AI LAYER: The Tactical Engine
 * A unified facade routing requests to the appropriate AI algorithm.
 */
object TacticalEngine {

    suspend fun getBestMove(
        opponentName: String,
        playerName: String,
        botMovesSoFar: List<Move>,
        context: Context,
        gameId: Int, // <-- NEW
        moriartyOffensivePrior: Array<FloatArray>? = null
    ): BotDecision {
        return withContext(Dispatchers.Default) {
            when {
                opponentName.contains("Moriarty", ignoreCase = true) -> MoriartyBot.getBestMove(botMovesSoFar, moriartyOffensivePrior, playerName, gameId) // <-- UPDATED
                opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getBestMove(botMovesSoFar, gameId) // <-- UPDATED
                opponentName.contains("PureDensity", ignoreCase = true) -> PureDensityBot.getBestMove(botMovesSoFar)
                opponentName.contains("DeepBlue", ignoreCase = true) -> DeepBlueBot.getBestMove(botMovesSoFar)
                opponentName.contains("Nemesis", ignoreCase = true) -> NemesisBot.getBestMove(botMovesSoFar, context)
                else -> {
                    val densityBot = DensityBot()
                    BotDecision(
                        coordinate = densityBot.getBestMove(botMovesSoFar),
                        log = "Standard Density Map",
                        heatMap = densityBot.getRawDensityMap(botMovesSoFar)
                    )
                }
            }
        }
    }

    fun generateBotFleet(opponentName: String, moriartyDefensivePrior: Array<FloatArray>? = null): List<Ship> {
        return if (opponentName.contains("Moriarty", ignoreCase = true)) {
            MoriartyBot.generatePhantomFleet(moriartyDefensivePrior)
        } else {
            generateRandomBotFleet()
        }
    }

    fun generateRandomBotFleet(): List<Ship> {
        val shipSpecs = listOf(Pair(5, "Carrier"), Pair(4, "Battleship"), Pair(3, "Cruiser"), Pair(3, "Submarine"), Pair(2, "Destroyer"))
        val fleet = mutableListOf<Ship>()
        val grid = Array(10) { BooleanArray(10) { false } }

        for ((size, name) in shipSpecs) {
            var placed = false
            while (!placed) {
                val isVertical = Random.nextBoolean()
                val x = Random.nextInt(if (isVertical) 10 else 10 - size + 1)
                val y = Random.nextInt(if (isVertical) 10 - size + 1 else 10)

                var collision = false
                for (i in 0 until size) {
                    val cx = x + if (!isVertical) i else 0
                    val cy = y + if (isVertical) i else 0
                    if (grid[cx][cy]) { collision = true; break }
                }

                if (!collision) {
                    for (i in 0 until size) {
                        val cx = x + if (!isVertical) i else 0
                        val cy = y + if (isVertical) i else 0
                        grid[cx][cy] = true
                    }
                    fleet.add(Ship(name = name, size = size, x = x.toFloat(), y = y.toFloat(), isVertical = isVertical, isPlaced = true))
                    placed = true
                }
            }
        }
        return fleet
    }

    fun getLiveDiagnostics(opponentName: String, moves: List<Move>): Pair<List<List<Pair<Int, Int>>>, List<Pair<Int, Int>>>? {
        return when {
            opponentName.contains("Moriarty", ignoreCase = true) -> MoriartyBot.getLiveDiagnostics(moves)
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveDiagnostics(moves)
            else -> null
        }
    }

    fun getLiveLivingFleet(opponentName: String, moves: List<Move>): List<Int>? {
        return when {
            opponentName.contains("Moriarty", ignoreCase = true) -> MoriartyBot.getLiveLivingFleet(moves)
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveLivingFleet(moves)
            else -> null
        }
    }

    fun getLiveHeatmap(opponentName: String, moves: List<Move>, context: Context, gameId: Int, moriartyOffensivePrior: Array<FloatArray>? = null): Array<IntArray>? { // <-- UPDATED
        return when {
            opponentName.contains("Moriarty", ignoreCase = true) -> MoriartyBot.getLiveHeatmap(moves, gameId, moriartyOffensivePrior) // <-- UPDATED
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveHeatmap(moves, gameId) // <-- UPDATED
            opponentName.contains("PureDensity", ignoreCase = true) -> PureDensityBot.getLiveHeatmap(moves)
            opponentName.contains("DeepBlue", ignoreCase = true) -> DeepBlueBot.getLiveHeatmap(moves)
            opponentName.contains("Nemesis", ignoreCase = true) -> NemesisBot.getLiveHeatmap(moves, context)
            else -> DensityBot().getLiveHeatmap(moves)
        }
    }
}

// ==========================================
// THE SHARED BRAIN: DEDUCTION ENGINE
// ==========================================

@VisibleForTesting
internal object DeductionEngine {
    const val CELL_UNKNOWN = 0
    const val CELL_MISS = -1
    const val CELL_HIT = 1
    const val CELL_SUNK = 2

    val originalFleet = listOf(5, 4, 3, 3, 2)

    data class BoardAnalysis(
        val activeHits: List<Pair<Int, Int>>,
        val deadShips: List<List<Pair<Int, Int>>>,
        val claimedHits: List<Pair<Int, Int>>,
        val deadSizes: List<Int>
    )

    fun getBoardState(moves: List<Move>): Array<IntArray> {
        val board = Array(10) { IntArray(10) { CELL_UNKNOWN } }
        moves.filter { !it.isOffense }.forEach { move ->
            if (move.result == "MISS") board[move.x][move.y] = CELL_MISS
            else if (move.isSunk) board[move.x][move.y] = CELL_SUNK
            else if (move.result == "HIT" || isShipData(move.result)) board[move.x][move.y] = CELL_HIT
        }
        return board
    }

    fun analyzeBoardState(board: Array<IntArray>, moves: List<Move>): BoardAnalysis {
        val allHits = mutableSetOf<Pair<Int, Int>>()
        val sunks = mutableListOf<Pair<Int, Int>>()
        val pegTimes = mutableMapOf<Pair<Int, Int>, Int>()

        // MISS filter prevents timeline contamination
        moves.filter { !it.isOffense && it.result != "MISS" }.forEach { move ->
            pegTimes[Pair(move.x, move.y)] = move.shotNumber
        }

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (board[x][y] == CELL_HIT) allHits.add(Pair(x, y))
                if (board[x][y] == CELL_SUNK) sunks.add(Pair(x, y))
            }
        }

        sunks.sortBy { pegTimes[it] ?: Int.MAX_VALUE }

        val result = solveBoard(board, sunks, pegTimes, originalFleet.toList(), emptyList(), emptyList(), allHits)

        val deadShips: List<List<Pair<Int, Int>>>
        val deadSizes: List<Int>
        val claimedHits: Set<Pair<Int, Int>>

        if (result != null) {
            deadShips = result.first
            deadSizes = result.second
            claimedHits = deadShips.flatten().toSet()
        } else {
            deadShips = sunks.map { listOf(it) }
            claimedHits = sunks.toSet()
            deadSizes = List(sunks.size) { 1 }
        }

        val activeHits = (allHits - claimedHits).toList()
        return BoardAnalysis(activeHits, deadShips, claimedHits.toList(), deadSizes)
    }

    private fun solveBoard(
        board: Array<IntArray>,
        sunks: List<Pair<Int, Int>>,
        pegTimes: Map<Pair<Int, Int>, Int>,
        availableFleet: List<Int>,
        currentDeadShips: List<List<Pair<Int, Int>>>,
        currentDeadSizes: List<Int>,
        allHits: Set<Pair<Int, Int>>
    ): Triple<List<List<Pair<Int, Int>>>, List<Int>, Int>? {
        if (sunks.isEmpty()) {
            val claimedSet = currentDeadShips.flatten().toSet()
            val leftovers = allHits - claimedSet

            if (availableFleet.isNotEmpty() && leftovers.isNotEmpty()) {
                val minShipSize = availableFleet.minOrNull() ?: 2

                for ((x, y) in leftovers) {
                    var left = x
                    while (left > 0 && board[left - 1][y] in listOf(CELL_UNKNOWN, CELL_HIT) && !claimedSet.contains(Pair(left - 1, y))) left--
                    var right = x
                    while (right < 9 && board[right + 1][y] in listOf(CELL_UNKNOWN, CELL_HIT) && !claimedSet.contains(Pair(right + 1, y))) right++
                    val horizLen = right - left + 1

                    var up = y
                    while (up > 0 && board[x][up - 1] in listOf(CELL_UNKNOWN, CELL_HIT) && !claimedSet.contains(Pair(x, up - 1))) up--
                    var down = y
                    while (down < 9 && board[x][down + 1] in listOf(CELL_UNKNOWN, CELL_HIT) && !claimedSet.contains(Pair(x, down + 1))) down++
                    val vertLen = down - up + 1

                    if (maxOf(horizLen, vertLen) < minShipSize) {
                        return null
                    }
                }
            }
            return Triple(currentDeadShips, currentDeadSizes, leftovers.size)
        }

        val sunk = sunks.first()
        val sunkTime = pegTimes[sunk] ?: Int.MAX_VALUE
        val uniqueSizes = availableFleet.distinct().sortedDescending()
        val currentClaimedSet = currentDeadShips.flatten().toSet()

        var bestResult: Triple<List<List<Pair<Int, Int>>>, List<Int>, Int>? = null
        var bestScore = Int.MAX_VALUE

        for (size in uniqueSizes) {
            val placements = getValidPlacements(board, sunk, sunkTime, size, pegTimes)

            for (placement in placements) {
                val placementSet = placement.toSet()

                if (placementSet.intersect(currentClaimedSet).isEmpty()) {
                    val newDeadShips = currentDeadShips + listOf(placement)
                    val newFleet = availableFleet.toMutableList().apply { remove(size) }
                    val newDeadSizes = currentDeadSizes + listOf(size)

                    val result = solveBoard(board, sunks.drop(1), pegTimes, newFleet, newDeadShips, newDeadSizes, allHits)

                    if (result != null) {
                        val score = result.third
                        if (score < bestScore) {
                            bestScore = score
                            bestResult = result
                        }
                    }
                }
            }
        }
        return bestResult
    }

    private fun getValidPlacements(
        board: Array<IntArray>, sunk: Pair<Int, Int>, sunkTime: Int, size: Int, pegTimes: Map<Pair<Int, Int>, Int>
    ): List<List<Pair<Int, Int>>> {
        val placements = mutableListOf<List<Pair<Int, Int>>>()
        val (sx, sy) = sunk

        for (xStart in (sx - size + 1)..sx) {
            var valid = true
            val pegList = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until size) {
                val x = xStart + i
                val y = sy
                if (x in 0..9 && y in 0..9) {
                    val currentPeg = Pair(x, y)
                    if (currentPeg == sunk) pegList.add(currentPeg)
                    else if (pegTimes.containsKey(currentPeg) && (pegTimes[currentPeg] ?: 0) < sunkTime) pegList.add(currentPeg)
                    else { valid = false; break }
                } else { valid = false; break }
            }
            if (valid) placements.add(pegList)
        }

        for (yStart in (sy - size + 1)..sy) {
            var valid = true
            val pegList = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until size) {
                val x = sx
                val y = yStart + i
                if (x in 0..9 && y in 0..9) {
                    val currentPeg = Pair(x, y)
                    if (currentPeg == sunk) pegList.add(currentPeg)
                    else if (pegTimes.containsKey(currentPeg) && (pegTimes[currentPeg] ?: 0) < sunkTime) pegList.add(currentPeg)
                    else { valid = false; break }
                } else { valid = false; break }
            }
            if (valid) placements.add(pegList)
        }
        return placements
    }

    // THE FIX: Add the random generator to the signature
    fun executeLinearKill(
        board: Array<IntArray>,
        activeHits: List<Pair<Int, Int>>,
        claimedHits: Set<Pair<Int, Int>>,
        random: kotlin.random.Random // <-- NEW
    ): Pair<Int, Int>? {
        val prunedHits = activeHits.filter { !claimedHits.contains(it) }
        val unvisited = prunedHits.toMutableSet()

        while (unvisited.isNotEmpty()) {
            val start = unvisited.first()
            val cluster = mutableSetOf<Pair<Int, Int>>()
            val queue = mutableListOf(start)

            while (queue.isNotEmpty()) {
                val current = queue.removeAt(0)
                if (cluster.add(current)) {
                    unvisited.remove(current)
                    val (x, y) = current
                    val neighbors = listOf(Pair(x - 1, y), Pair(x + 1, y), Pair(x, y - 1), Pair(x, y + 1))
                    for (n in neighbors) {
                        if (prunedHits.contains(n) && !cluster.contains(n)) queue.add(n)
                    }
                }
            }

            val xs = cluster.map { it.first }.toSet()
            val ys = cluster.map { it.second }.toSet()

            if (cluster.size == 1) {
                val validTargets = getUnknownNeighbors(board, cluster.first())
                if (validTargets.isNotEmpty()) return validTargets.random(random) // <-- UPDATED

            } else if (ys.size == 1) {
                val y = ys.first()
                val endpoints = mutableListOf<Pair<Int, Int>>()

                var xMin = xs.minOrNull() ?: 0
                while (xMin >= 0 && board[xMin][y] == CELL_HIT) xMin--
                if (xMin >= 0 && board[xMin][y] == CELL_UNKNOWN) endpoints.add(Pair(xMin, y))

                var xMax = xs.maxOrNull() ?: 0
                while (xMax <= 9 && board[xMax][y] == CELL_HIT) xMax++
                if (xMax <= 9 && board[xMax][y] == CELL_UNKNOWN) endpoints.add(Pair(xMax, y))

                if (endpoints.isNotEmpty()) return endpoints.random(random) // <-- UPDATED

            } else if (xs.size == 1) {
                val x = xs.first()
                val endpoints = mutableListOf<Pair<Int, Int>>()

                var yMin = ys.minOrNull() ?: 0
                while (yMin >= 0 && board[x][yMin] == CELL_HIT) yMin--
                if (yMin >= 0 && board[x][yMin] == CELL_UNKNOWN) endpoints.add(Pair(x, yMin))

                var yMax = ys.maxOrNull() ?: 0
                while (yMax <= 9 && board[x][yMax] == CELL_HIT) yMax++
                if (yMax <= 9 && board[x][yMax] == CELL_UNKNOWN) endpoints.add(Pair(x, yMax))

                if (endpoints.isNotEmpty()) return endpoints.random(random) // <-- UPDATED

            } else {
                val validEndpoints = mutableListOf<Pair<Int, Int>>()
                for ((x, y) in cluster) {
                    for ((dx, dy) in listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))) {
                        if (cluster.contains(Pair(x + dx, y + dy))) {
                            var currX = x
                            var currY = y
                            while (cluster.contains(Pair(currX, currY))) {
                                currX += dx
                                currY += dy
                            }
                            if (currX in 0..9 && currY in 0..9 && board[currX][currY] == CELL_UNKNOWN) {
                                validEndpoints.add(Pair(currX, currY))
                            }
                        }
                    }
                }
                if (validEndpoints.isNotEmpty()) return validEndpoints.random(random) // <-- UPDATED
            }

            for (hit in cluster) {
                val validTargets = getUnknownNeighbors(board, hit)
                if (validTargets.isNotEmpty()) return validTargets.random(random) // <-- UPDATED
            }
        }
        return null
    }

    fun getUnknownNeighbors(board: Array<IntArray>, peg: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = peg
        return listOf(Pair(x - 1, y), Pair(x + 1, y), Pair(x, y - 1), Pair(x, y + 1))
            .filter { it.first in 0..9 && it.second in 0..9 && board[it.first][it.second] == CELL_UNKNOWN }
    }

    fun getPerpendicularEnds(shipPegs: List<Pair<Int, Int>>, board: Array<IntArray>): List<Pair<Int, Int>> {
        val targets = mutableListOf<Pair<Int, Int>>()
        if (shipPegs.size < 2) return targets

        val sortedPegs = shipPegs.sortedWith(compareBy({ it.first }, { it.second }))
        val (x1, y1) = sortedPegs.first()
        val (x2, y2) = sortedPegs.last()

        val potentials = if (x1 == x2) {
            listOf(Pair(x1 - 1, y1), Pair(x1 + 1, y1), Pair(x2 - 1, y2), Pair(x2 + 1, y2))
        } else {
            listOf(Pair(x1, y1 - 1), Pair(x1, y1 + 1), Pair(x2, y2 - 1), Pair(x2, y2 + 1))
        }

        for ((x, y) in potentials) {
            if (x in 0..9 && y in 0..9 && board[x][y] == CELL_UNKNOWN) targets.add(Pair(x, y))
        }
        return targets
    }

    fun getHeatmap(board: Array<IntArray>, deadSizes: List<Int>, useParity: Boolean, parityOffset: Int): Array<IntArray> {
        val heatMap = Array(10) { IntArray(10) { 0 } }
        val livingFleet = originalFleet.toMutableList()

        for (ds in deadSizes) {
            val sizeToRemove = if (ds > 5) 5 else ds
            livingFleet.remove(sizeToRemove)
        }

        if (livingFleet.isEmpty()) livingFleet.add(2)
        val uniqueShips = livingFleet.distinct()

        for (shipSize in uniqueShips) {
            for (x in 0 until 10) {
                for (y in 0..10 - shipSize) {
                    evaluateHuntPlacement(board, heatMap, x, y, shipSize, isHorizontal = false, useParity, parityOffset)
                }
            }
            for (x in 0..10 - shipSize) {
                for (y in 0 until 10) {
                    evaluateHuntPlacement(board, heatMap, x, y, shipSize, isHorizontal = true, useParity, parityOffset)
                }
            }
        }
        return heatMap
    }

    private fun evaluateHuntPlacement(
        board: Array<IntArray>, heatMap: Array<IntArray>,
        startX: Int, startY: Int, size: Int, isHorizontal: Boolean, useParity: Boolean, parityOffset: Int
    ) {
        for (i in 0 until size) {
            val cx = if (isHorizontal) startX + i else startX
            val cy = if (isHorizontal) startY else startY + i
            if (board[cx][cy] in listOf(CELL_MISS, CELL_SUNK, CELL_HIT)) return
        }

        for (i in 0 until size) {
            val cx = if (isHorizontal) startX + i else startX
            val cy = if (isHorizontal) startY else startY + i
            if (board[cx][cy] == CELL_UNKNOWN) {
                if (useParity) {
                    heatMap[cx][cy] += if ((cx + cy) % 2 == parityOffset) 0 else 1
                } else {
                    heatMap[cx][cy] += 1
                }
            }
        }
    }
}

// ==========================================
// BOT 1: SHERLOCK (GAME THEORY OPTIMAL)
// ==========================================

private object SherlockBot {

    fun getBestMove(moves: List<Move>, gameId: Int): BotDecision {
        val parityOffset = gameId % 2
        val deterministicRandom = kotlin.random.Random((gameId * 10000) + moves.size)

        val board = DeductionEngine.getBoardState(moves)
        val heatMap = Array(10) { IntArray(10) { 0 } }
        val diagnosticMap = Array(10) { IntArray(10) { 0 } }

        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        analysis.claimedHits.forEach { diagnosticMap[it.first][it.second] = 1 }
        analysis.activeHits.forEach { diagnosticMap[it.first][it.second] = 3 }

        if (analysis.activeHits.isNotEmpty()) {
            val target = DeductionEngine.executeLinearKill(board, analysis.activeHits, analysis.claimedHits.toSet(), deterministicRandom)
            if (target != null) return BotDecision(target, "Sherlock: Tracing active vector.", heatMap, diagnosticMap)
        }

        val target = executeGravitationalHunt(board, analysis.deadSizes, analysis.deadShips, parityOffset, deterministicRandom)
        val visualHeatmap = DeductionEngine.getHeatmap(board, analysis.deadSizes, true, parityOffset)
        return BotDecision(target, "Sherlock: Gravitational Sweeper.", visualHeatmap, diagnosticMap)
    }

    private fun executeGravitationalHunt(
        board: Array<IntArray>,
        deadSizes: List<Int>,
        deadShips: List<List<Pair<Int, Int>>>,
        parityOffset: Int,
        random: kotlin.random.Random
    ): Pair<Int, Int> {
        val heatMap = DeductionEngine.getHeatmap(board, deadSizes, useParity = true, parityOffset)
        var maxHeat = -1
        var bestMoves = mutableListOf<Pair<Int, Int>>()

        for (x in 0..9) {
            for (y in 0..9) {
                if (board[x][y] == DeductionEngine.CELL_UNKNOWN) {
                    val heat = heatMap[x][y]
                    if (heat > maxHeat) { maxHeat = heat; bestMoves = mutableListOf(Pair(x, y)) }
                    else if (heat == maxHeat) bestMoves.add(Pair(x, y))
                }
            }
        }
        if (maxHeat > 0 && bestMoves.isNotEmpty()) return bestMoves.random(random)

        val threeShipEnds = mutableListOf<Pair<Int, Int>>()
        for (ship in deadShips) if (ship.size == 3) threeShipEnds.addAll(DeductionEngine.getPerpendicularEnds(ship, board))
        if (threeShipEnds.isNotEmpty()) return threeShipEnds.random(random)

        val otherShipEnds = mutableListOf<Pair<Int, Int>>()
        for (ship in deadShips) if (ship.size != 3) otherShipEnds.addAll(DeductionEngine.getPerpendicularEnds(ship, board))
        if (otherShipEnds.isNotEmpty()) return otherShipEnds.random(random)

        val threePerimeter = mutableListOf<Pair<Int, Int>>()
        for (ship in deadShips) {
            if (ship.size == 3) {
                for ((x, y) in ship) {
                    for ((dx, dy) in listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))) {
                        val nx = x + dx; val ny = y + dy
                        if (nx in 0..9 && ny in 0..9 && board[nx][ny] == DeductionEngine.CELL_UNKNOWN) threePerimeter.add(Pair(nx, ny))
                    }
                }
            }
        }
        if (threePerimeter.isNotEmpty()) return threePerimeter.random(random)

        val otherPerimeter = mutableListOf<Pair<Int, Int>>()
        for (ship in deadShips) {
            if (ship.size != 3) {
                for ((x, y) in ship) {
                    for ((dx, dy) in listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))) {
                        val nx = x + dx; val ny = y + dy
                        if (nx in 0..9 && ny in 0..9 && board[nx][ny] == DeductionEngine.CELL_UNKNOWN) otherPerimeter.add(Pair(nx, ny))
                    }
                }
            }
        }
        if (otherPerimeter.isNotEmpty()) return otherPerimeter.random(random)

        val heatMapEm = DeductionEngine.getHeatmap(board, listOf(5, 4, 3, 3), useParity = false, parityOffset)
        var maxHeatEm = -1
        var bestMovesEm = mutableListOf<Pair<Int, Int>>()
        for (x in 0..9) {
            for (y in 0..9) {
                if (board[x][y] == DeductionEngine.CELL_UNKNOWN) {
                    val heat = heatMapEm[x][y]
                    if (heat > maxHeatEm) { maxHeatEm = heat; bestMovesEm = mutableListOf(Pair(x, y)) }
                    else if (heat == maxHeatEm) bestMovesEm.add(Pair(x, y))
                }
            }
        }
        if (maxHeatEm > 0 && bestMovesEm.isNotEmpty()) return bestMovesEm.random(random)

        val openWater = mutableListOf<Pair<Int, Int>>()
        for (x in 0..9) for (y in 0..9) if (board[x][y] == DeductionEngine.CELL_UNKNOWN) openWater.add(Pair(x, y))
        return if (openWater.isNotEmpty()) openWater.random(random) else Pair(0, 0)
    }

    // THESE WERE ACCIDENTALLY DELETED - RESTORED HERE:
    fun getLiveDiagnostics(moves: List<Move>): Pair<List<List<Pair<Int, Int>>>, List<Pair<Int, Int>>> {
        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        return Pair(analysis.deadShips, analysis.activeHits)
    }

    fun getLiveLivingFleet(moves: List<Move>): List<Int> {
        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        val livingFleet = DeductionEngine.originalFleet.toMutableList()
        for (deadSize in analysis.deadSizes) livingFleet.remove(if (deadSize > 5) 5 else deadSize)
        if (livingFleet.isEmpty()) livingFleet.add(2)
        return livingFleet.sortedDescending()
    }

    fun getLiveHeatmap(moves: List<Move>, gameId: Int): Array<IntArray> {
        val parityOffset = gameId % 2
        val deterministicRandom = kotlin.random.Random((gameId * 10000) + moves.size)

        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)

        if (analysis.activeHits.isNotEmpty()) {
            val visualHeatMap = Array(10) { IntArray(10) { 0 } }
            val target = DeductionEngine.executeLinearKill(board, analysis.activeHits, analysis.claimedHits.toSet(), deterministicRandom)
            if (target != null) {
                visualHeatMap[target.first][target.second] = 100
                return visualHeatMap
            }
        }

        return DeductionEngine.getHeatmap(board, analysis.deadSizes, useParity = true, parityOffset)
    }
}

// ==========================================
// BOT 2: MORIARTY (EXPLOITATIVE BAYESIAN)
// ==========================================

private object MoriartyBot {

    fun getBestMove(moves: List<Move>, offensivePrior: Array<FloatArray>?, playerName: String, gameId: Int): BotDecision {
        val parityOffset = gameId % 2
        val deterministicRandom = kotlin.random.Random((gameId * 10000) + moves.size)

        val board = DeductionEngine.getBoardState(moves)
        val heatMap = Array(10) { IntArray(10) { 0 } }
        val diagnosticMap = Array(10) { IntArray(10) { 0 } }

        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        analysis.claimedHits.forEach { diagnosticMap[it.first][it.second] = 1 }
        analysis.activeHits.forEach { diagnosticMap[it.first][it.second] = 3 }

        val safePrior = offensivePrior ?: Array(10) { FloatArray(10) { 1.0f } }
        val flatWeights = safePrior.flatMap { it.toList() }
        val maxBias = flatWeights.maxOrNull() ?: 1.0f
        val minBias = flatWeights.minOrNull() ?: 1.0f

        val isUniformFallback = offensivePrior == null || (maxBias == 1.0f && minBias == 1.0f)
        val matrixTypeTag = if (isUniformFallback) "Fallback (Uniform 1.0)" else "moriarty_offense_$playerName"

        if (analysis.activeHits.isNotEmpty()) {
            val target = DeductionEngine.executeLinearKill(board, analysis.activeHits, analysis.claimedHits.toSet(), deterministicRandom)
            if (target != null) {
                val logMessage = buildString {
                    append("Target: $playerName | State: Tracing Vector\n")
                    append("Data Array: $matrixTypeTag\n")
                    append("Bias Skew: [Max: ${String.format("%.2f", maxBias)}x, Min: ${String.format("%.2f", minBias)}x]\n")
                    append("Peak Node Weight: N/A (Linear Kill)")
                }
                return BotDecision(target, logMessage, heatMap, diagnosticMap)
            }
        }

        val (target, peakBayesianHeat) = executeBayesianHuntDetailed(board, analysis.deadSizes, analysis.deadShips, safePrior, heatMap, parityOffset, deterministicRandom)

        val logMessage = buildString {
            append("Target: $playerName | State: Bayesian Hunt\n")
            append("Data Array: $matrixTypeTag\n")
            append("Bias Skew: [Max: ${String.format("%.2f", maxBias)}x, Min: ${String.format("%.2f", minBias)}x]\n")
            append("Peak Node Weight: ${String.format("%.1f", peakBayesianHeat)}")
        }

        return BotDecision(target, logMessage, heatMap, diagnosticMap)
    }

    private fun executeBayesianHuntDetailed(
        board: Array<IntArray>,
        deadSizes: List<Int>,
        deadShips: List<List<Pair<Int, Int>>>,
        offensivePrior: Array<FloatArray>,
        visualHeatMap: Array<IntArray>,
        parityOffset: Int,
        random: kotlin.random.Random
    ): Pair<Pair<Int, Int>, Float> {
        val baselineHeatMap = DeductionEngine.getHeatmap(board, deadSizes, useParity = true, parityOffset)

        var maxHeat = -1f
        var bestMoves = mutableListOf<Pair<Int, Int>>()

        for (x in 0..9) {
            for (y in 0..9) {
                if (board[x][y] == DeductionEngine.CELL_UNKNOWN) {
                    val baseScore = baselineHeatMap[x][y].toFloat()
                    val humanBias = offensivePrior[x][y]

                    val finalScore = baseScore * humanBias
                    visualHeatMap[x][y] = (finalScore * 10f).toInt()

                    if (finalScore > maxHeat) {
                        maxHeat = finalScore
                        bestMoves = mutableListOf(Pair(x, y))
                    } else if (finalScore == maxHeat) {
                        bestMoves.add(Pair(x, y))
                    }
                }
            }
        }

        val chosenCoordinate = if (maxHeat > 0f && bestMoves.isNotEmpty()) {
            bestMoves.random(random)
        } else {
            val openWater = mutableListOf<Pair<Int, Int>>()
            for (x in 0..9) for (y in 0..9) if (board[x][y] == DeductionEngine.CELL_UNKNOWN) openWater.add(Pair(x, y))
            if (openWater.isNotEmpty()) openWater.random(random) else Pair(0, 0)
        }

        return Pair(chosenCoordinate, maxHeat)
    }

    fun generatePhantomFleet(defensivePrior: Array<FloatArray>?): List<Ship> {
        val safePrior = defensivePrior ?: Array(10) { FloatArray(10) { 1.0f } }
        val shipSpecs = listOf(Pair(5, "Carrier"), Pair(4, "Battleship"), Pair(3, "Cruiser"), Pair(3, "Submarine"), Pair(2, "Destroyer"))
        val fleet = mutableListOf<Ship>()
        val grid = Array(10) { BooleanArray(10) { false } }

        // DYNAMIC HEAT: Prevents ships from clumping together
        val dynamicHeat = Array(10) { x -> FloatArray(10) { y -> safePrior[x][y] } }

        for ((size, name) in shipSpecs) {
            val validPlacements = mutableListOf<Triple<Int, Int, Boolean>>()
            val placementWeights = mutableListOf<Float>()

            // 1. Find all valid spots and calculate their heat
            for (isVertical in listOf(true, false)) {
                val maxX = if (isVertical) 10 else 10 - size + 1
                val maxY = if (isVertical) 10 - size + 1 else 10

                for (x in 0 until maxX) {
                    for (y in 0 until maxY) {
                        var canPlace = true
                        var heatSum = 0.0f
                        for (i in 0 until size) {
                            val cx = x + if (!isVertical) i else 0
                            val cy = y + if (isVertical) i else 0
                            if (grid[cx][cy]) { canPlace = false; break }
                            heatSum += dynamicHeat[cx][cy]
                        }

                        if (canPlace) {
                            validPlacements.add(Triple(x, y, isVertical))
                            placementWeights.add(heatSum)
                        }
                    }
                }
            }

            // 2. Select a spot using an Inverse Roulette Wheel (High Heat = Low Probability)
            if (validPlacements.isNotEmpty()) {
                val maxHeat = placementWeights.maxOrNull() ?: 1.0f
                val invertedWeights = placementWeights.map { (maxHeat - it) + 0.1f } // 0.1f ensures no spot is 0%
                val totalWeight = invertedWeights.sum()

                var randomValue = kotlin.random.Random.nextFloat() * totalWeight
                var chosenIndex = validPlacements.lastIndex

                for (i in invertedWeights.indices) {
                    randomValue -= invertedWeights[i]
                    if (randomValue <= 0f) { chosenIndex = i; break }
                }

                // 3. Place the ship and apply the dynamic spacing penalty
                val (px, py, bestIsVertical) = validPlacements[chosenIndex]
                for (i in 0 until size) {
                    val cx = px + if (!bestIsVertical) i else 0
                    val cy = py + if (bestIsVertical) i else 0
                    grid[cx][cy] = true

                    // Spacing Penalty: Make surrounding cells "hot" so the next ship avoids them
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = cx + dx; val ny = cy + dy
                            if (nx in 0..9 && ny in 0..9) dynamicHeat[nx][ny] += 2.0f
                        }
                    }
                }
                fleet.add(Ship(name = name, size = size, x = px.toFloat(), y = py.toFloat(), isVertical = bestIsVertical, isPlaced = true))
            }
        }
        return fleet
    }

    fun getLiveDiagnostics(moves: List<Move>): Pair<List<List<Pair<Int, Int>>>, List<Pair<Int, Int>>> {
        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        return Pair(analysis.deadShips, analysis.activeHits)
    }

    fun getLiveLivingFleet(moves: List<Move>): List<Int> {
        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        val livingFleet = DeductionEngine.originalFleet.toMutableList()
        for (deadSize in analysis.deadSizes) livingFleet.remove(if (deadSize > 5) 5 else deadSize)
        if (livingFleet.isEmpty()) livingFleet.add(2)
        return livingFleet.sortedDescending()
    }

    fun getLiveHeatmap(moves: List<Move>, gameId: Int, offensivePrior: Array<FloatArray>?): Array<IntArray> {
        val parityOffset = gameId % 2
        val deterministicRandom = kotlin.random.Random((gameId * 10000) + moves.size)

        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)

        val visualHeatMap = Array(10) { IntArray(10) { 0 } }

        if (analysis.activeHits.isNotEmpty()) {
            val target = DeductionEngine.executeLinearKill(board, analysis.activeHits, analysis.claimedHits.toSet(), deterministicRandom)
            if (target != null) {
                visualHeatMap[target.first][target.second] = 100
                return visualHeatMap
            }
        }

        val safePrior = offensivePrior ?: Array(10) { FloatArray(10) { 1.0f } }
        executeBayesianHuntDetailed(board, analysis.deadSizes, analysis.deadShips, safePrior, visualHeatMap, parityOffset, deterministicRandom)
        return visualHeatMap
    }
}

// ==========================================
// BOT 3: PURE DENSITY (GEOMETRIC MATH)
// ==========================================

private object PureDensityBot {
    private const val CELL_UNKNOWN = 0
    private const val CELL_MISS = -1
    private const val CELL_HIT = 1
    private val fleetSizes = listOf(5, 4, 3, 3, 2)

    fun getBestMove(botMoves: List<Move>): BotDecision {
        val board = getBoardState(botMoves)
        val densityMap = getRawDensityMap(board)
        val shotCells = botMoves.map { Pair(it.x, it.y) }.toSet()

        var maxDensity = -1
        val bestMoves = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (!shotCells.contains(Pair(x, y))) {
                    val currentDensity = densityMap[x][y]
                    if (currentDensity > maxDensity) {
                        maxDensity = currentDensity
                        bestMoves.clear()
                        bestMoves.add(Pair(x, y))
                    } else if (currentDensity == maxDensity) {
                        bestMoves.add(Pair(x, y))
                    }
                }
            }
        }
        val target = if (bestMoves.isNotEmpty()) bestMoves.random() else Pair(0, 0)
        return BotDecision(target, "Pure Density Math", densityMap)
    }

    private fun getBoardState(moves: List<Move>): Array<IntArray> {
        val board = Array(10) { IntArray(10) { CELL_UNKNOWN } }
        moves.filter { !it.isOffense }.forEach { move ->
            if (move.result == "MISS") board[move.x][move.y] = CELL_MISS
            else if (move.result == "HIT" || move.isSunk) board[move.x][move.y] = CELL_HIT
        }
        return board
    }

    private fun getRawDensityMap(board: Array<IntArray>): Array<IntArray> {
        val densityMap = Array(10) { IntArray(10) { 0 } }
        for (size in fleetSizes.distinct()) {
            for (x in 0 until 10) {
                for (y in 0 until 10) {
                    if (x + size <= 10) {
                        var canFit = true
                        for (i in 0 until size) { if (board[x + i][y] == CELL_MISS) { canFit = false; break } }
                        if (canFit) {
                            for (i in 0 until size) { if (board[x + i][y] == CELL_UNKNOWN) densityMap[x + i][y] += 1 }
                        }
                    }
                    if (y + size <= 10) {
                        var canFit = true
                        for (i in 0 until size) { if (board[x][y + i] == CELL_MISS) { canFit = false; break } }
                        if (canFit) {
                            for (i in 0 until size) { if (board[x][y + i] == CELL_UNKNOWN) densityMap[x][y + i] += 1 }
                        }
                    }
                }
            }
        }
        return densityMap
    }

    fun getLiveHeatmap(moves: List<Move>): Array<IntArray> {
        val board = getBoardState(moves)
        return getRawDensityMap(board)
    }
}

// ==========================================
// BOT 4: STANDARD DENSITY (FALLBACK)
// ==========================================

private class DensityBot {
    private val CELL_UNKNOWN = 0
    private val CELL_MISS = -1
    private val CELL_HIT = 1
    private val CELL_SUNK = -2
    private val initialFleetSizes = listOf(5, 4, 3, 3, 2)
    private data class Point(val x: Int, val y: Int)

    fun getBestMove(botMoves: List<Move>): Pair<Int, Int> {
        val intBoard = getBoardState(botMoves)
        val densityMap = getRawDensityMap(botMoves)

        var bestHuntScore = 0
        val bestHuntMoves = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (intBoard[x][y] == CELL_HIT) {
                    for (dir in directions) {
                        var currentX = x + dir.first
                        var currentY = y + dir.second
                        var rayScore = 1

                        while (currentX in 0..9 && currentY in 0..9 && intBoard[currentX][currentY] == CELL_HIT) {
                            rayScore++
                            currentX += dir.first
                            currentY += dir.second
                        }

                        if (currentX in 0..9 && currentY in 0..9 && intBoard[currentX][currentY] == CELL_UNKNOWN) {
                            val target = Pair(currentX, currentY)
                            if (rayScore > bestHuntScore) {
                                bestHuntScore = rayScore
                                bestHuntMoves.clear()
                                bestHuntMoves.add(target)
                            } else if (rayScore == bestHuntScore) {
                                if (!bestHuntMoves.contains(target)) bestHuntMoves.add(target)
                            }
                        }
                    }
                }
            }
        }

        if (bestHuntMoves.isNotEmpty()) {
            return bestHuntMoves.maxByOrNull { densityMap[it.first][it.second] } ?: bestHuntMoves.random()
        }

        var maxDensity = -1
        val bestDensityMoves = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (intBoard[x][y] == CELL_UNKNOWN) {
                    if ((x + y) % 2 != 0) continue

                    var currentDensity = densityMap[x][y]
                    if (x == 0 || x == 9 || y == 0 || y == 9) currentDensity = (currentDensity * 1.2).toInt()

                    if (currentDensity > maxDensity) {
                        maxDensity = currentDensity
                        bestDensityMoves.clear()
                        bestDensityMoves.add(Pair(x, y))
                    } else if (currentDensity == maxDensity) {
                        bestDensityMoves.add(Pair(x, y))
                    }
                }
            }
        }

        if (bestDensityMoves.isEmpty()) {
            for (x in 0 until 10) {
                for (y in 0 until 10) {
                    if (intBoard[x][y] == CELL_UNKNOWN) {
                        var currentDensity = densityMap[x][y]
                        if (x == 0 || x == 9 || y == 0 || y == 9) currentDensity = (currentDensity * 1.2).toInt()

                        if (currentDensity > maxDensity) {
                            maxDensity = currentDensity
                            bestDensityMoves.clear()
                            bestDensityMoves.add(Pair(x, y))
                        } else if (currentDensity == maxDensity) {
                            bestDensityMoves.add(Pair(x, y))
                        }
                    }
                }
            }
        }

        return if (bestDensityMoves.isNotEmpty()) bestDensityMoves.random() else Pair(0, 0)
    }

    fun getRawDensityMap(moves: List<Move>): Array<IntArray> {
        val botMoves = moves.filter { !it.isOffense }
        val board = getBoardState(botMoves)
        val remainingShips = initialFleetSizes.toMutableList()
        val densityMap = Array(10) { IntArray(10) { 0 } }

        for (size in remainingShips.distinct()) {
            for (x in 0 until 10) {
                for (y in 0 until 10) {
                    if (canFit(board, x, y, size, isHorizontal = true)) {
                        val weight = calculateHypothesisWeight(board, x, y, size, isHorizontal = true)
                        for (i in 0 until size) {
                            if (board[x + i][y] == CELL_UNKNOWN) densityMap[x + i][y] += weight
                        }
                    }
                    if (canFit(board, x, y, size, isHorizontal = false)) {
                        val weight = calculateHypothesisWeight(board, x, y, size, isHorizontal = false)
                        for (i in 0 until size) {
                            if (board[x][y + i] == CELL_UNKNOWN) densityMap[x][y + i] += weight
                        }
                    }
                }
            }
        }
        return densityMap
    }

    private fun getBoardState(moves: List<Move>): Array<IntArray> {
        val board = Array(10) { IntArray(10) { CELL_UNKNOWN } }
        val remainingShips = initialFleetSizes.toMutableList()

        moves.forEach { move ->
            if (move.result == "MISS") {
                board[move.x][move.y] = CELL_MISS
            } else if (move.result == "HIT") {
                board[move.x][move.y] = CELL_HIT
                if (move.isSunk) {
                    val extractedShip = extractShipFromCluster(board, Point(move.x, move.y), remainingShips)
                    extractedShip.forEach { p -> board[p.x][p.y] = CELL_SUNK }
                    val matchedSize = remainingShips.minByOrNull { abs(it - extractedShip.size) }
                    if (matchedSize != null) remainingShips.remove(matchedSize)
                }
            }
        }
        return board
    }

    private fun extractShipFromCluster(board: Array<IntArray>, sunkPeg: Point, remainingShips: List<Int>): List<Point> {
        val horizontal = mutableListOf(sunkPeg)
        var left = sunkPeg.x - 1
        while (left >= 0 && board[left][sunkPeg.y] == CELL_HIT) { horizontal.add(0, Point(left, sunkPeg.y)); left-- }
        var right = sunkPeg.x + 1
        while (right < 10 && board[right][sunkPeg.y] == CELL_HIT) { horizontal.add(Point(right, sunkPeg.y)); right++ }

        val vertical = mutableListOf(sunkPeg)
        var up = sunkPeg.y - 1
        while (up >= 0 && board[sunkPeg.x][up] == CELL_HIT) { vertical.add(0, Point(sunkPeg.x, up)); up-- }
        var down = sunkPeg.y + 1
        while (down < 10 && board[sunkPeg.x][down] == CELL_HIT) { vertical.add(Point(sunkPeg.x, down)); down++ }

        val bestHoriz = remainingShips.filter { it <= horizontal.size }.maxOrNull() ?: 0
        val bestVert = remainingShips.filter { it <= vertical.size }.maxOrNull() ?: 0

        val isHoriz = bestHoriz >= bestVert && bestHoriz > 0
        val targetLine = if (isHoriz) horizontal else vertical
        val finalSize = if (isHoriz) bestHoriz else if (bestVert > 0) bestVert else 1

        val sunkIndex = targetLine.indexOf(sunkPeg)
        var startIndex = sunkIndex - finalSize + 1
        if (startIndex < 0) startIndex = 0

        var endIndex = startIndex + finalSize
        if (endIndex > targetLine.size) {
            endIndex = targetLine.size
            startIndex = endIndex - finalSize
            if (startIndex < 0) startIndex = 0
        }

        return targetLine.subList(startIndex, endIndex)
    }

    private fun canFit(board: Array<IntArray>, x: Int, y: Int, size: Int, isHorizontal: Boolean): Boolean {
        if (isHorizontal) {
            if (x + size > 10) return false
            for (i in 0 until size) { if (board[x + i][y] == CELL_MISS || board[x + i][y] == CELL_SUNK) return false }
        } else {
            if (y + size > 10) return false
            for (i in 0 until size) { if (board[x][y + i] == CELL_MISS || board[x][y + i] == CELL_SUNK) return false }
        }
        return true
    }

    private fun calculateHypothesisWeight(board: Array<IntArray>, x: Int, y: Int, size: Int, isHorizontal: Boolean): Int {
        var overlapHits = 0
        for (i in 0 until size) {
            val cx = if (isHorizontal) x + i else x
            val cy = if (isHorizontal) y else y + i
            if (board[cx][cy] == CELL_HIT) overlapHits++
        }
        return if (overlapHits > 0) 500 * overlapHits else 1
    }

    fun getLiveHeatmap(moves: List<Move>): Array<IntArray> {
        return getRawDensityMap(moves)
    }
}

// ==========================================
// BOT 5: NEMESIS (REINFORCEMENT LEARNING)
// ==========================================

private object NemesisBot {
    private const val CELL_UNKNOWN = 0
    private const val CELL_MISS = 1
    private const val CELL_HIT = 2
    private const val CELL_SUNK = 3

    private var cachedModelBuffer: MappedByteBuffer? = null

    private fun getModelBuffer(context: Context): MappedByteBuffer {
        if (cachedModelBuffer == null) {
            val fd = context.assets.openFd("nemesis_rl_production.tflite")
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                cachedModelBuffer = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
        return cachedModelBuffer!!
    }

    fun getBestMove(botMoves: List<Move>, context: Context): BotDecision {
        val interpreter = Interpreter(getModelBuffer(context))
        val board = getBoardState(botMoves)

        val inputBuffer = ByteBuffer.allocateDirect(10 * 10 * 1 * 4).apply { order(ByteOrder.nativeOrder()) }

        for (y in 0 until 10) {
            for (x in 0 until 10) {
                val pixelValue = when (board[x][y]) {
                    CELL_UNKNOWN -> 0.0f
                    CELL_MISS    -> 85.0f
                    CELL_HIT     -> 170.0f
                    CELL_SUNK    -> 255.0f
                    else         -> 0.0f
                }
                inputBuffer.putFloat(pixelValue)
            }
        }

        val outputBuffer = Array(1) { FloatArray(100) }
        inputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        interpreter.close()

        val logits = outputBuffer[0]
        val intHeatmap = Array(10) { IntArray(10) { 0 } }
        var bestX = -1
        var bestY = -1
        var highestLogit = -Float.MAX_VALUE

        val minLogit = logits.minOrNull() ?: 0f
        val maxLogit = logits.maxOrNull() ?: 1f
        val logitRange = if (maxLogit - minLogit == 0f) 1f else maxLogit - minLogit

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                val flatIndex = y * 10 + x
                val logitValue = logits[flatIndex]
                val normalizedScore = ((logitValue - minLogit) / logitRange) * 100
                intHeatmap[x][y] = normalizedScore.toInt()

                if (board[x][y] == CELL_UNKNOWN && logitValue > highestLogit) {
                    highestLogit = logitValue
                    bestX = x
                    bestY = y
                }
            }
        }

        if (bestX == -1) bestX = 0
        if (bestY == -1) bestY = 0

        return BotDecision(Pair(bestX, bestY), "Nemesis RL Engine Engaged. Node weight: $highestLogit", intHeatmap)
    }

    private fun getBoardState(moves: List<Move>): Array<IntArray> {
        val board = Array(10) { IntArray(10) { CELL_UNKNOWN } }
        moves.filter { !it.isOffense }.forEach { move ->
            if (move.result == "MISS") {
                board[move.x][move.y] = CELL_MISS
            } else if (move.isSunk) {
                board[move.x][move.y] = CELL_SUNK
            } else if (move.result == "HIT" || isShipData(move.result)) {
                board[move.x][move.y] = CELL_HIT
            }
        }
        return board
    }

    fun getLiveHeatmap(moves: List<Move>, context: Context): Array<IntArray>? {
        return getBestMove(moves, context).heatMap
    }
}

// ==========================================
// BOT 6: DEEP BLUE (MONTE CARLO SEARCH)
// ==========================================

private object DeepBlueBot {
    private data class Placement(val startIdx: Int, val isVertical: Boolean)

    fun getBestMove(botMoves: List<Move>): BotDecision {
        return runMonteCarlo(botMoves, targetSimulations = 2000)
    }

    private fun runMonteCarlo(moves: List<Move>, targetSimulations: Int): BotDecision {
        val missCells = BooleanArray(100) { false }
        val hitCells = BooleanArray(100) { false }
        val sunkCells = BooleanArray(100) { false }
        var totalHits = 0

        moves.forEach {
            val idx = it.y * 10 + it.x
            if (it.result == "MISS") missCells[idx] = true
            if (it.result == "HIT") { hitCells[idx] = true; totalHits++ }
            if (it.isSunk) { sunkCells[idx] = true; hitCells[idx] = true; totalHits++ }
        }

        val shipSizes = listOf(5, 4, 3, 3, 2)
        val heatMap = IntArray(100) { 0 }
        var validUniverses = 0
        val startTime = System.currentTimeMillis()
        val board = IntArray(100) { -1 }

        fun getDynamicPlacements(size: Int): List<Placement> {
            val valid = mutableListOf<Placement>()
            for (idx in 0 until 100) {
                val x = idx % 10
                val y = idx / 10
                if (x + size <= 10) {
                    var canPlace = true
                    for (k in 0 until size) { if (missCells[idx + k] || board[idx + k] != -1) { canPlace = false; break } }
                    if (canPlace) valid.add(Placement(idx, false))
                }
                if (y + size <= 10) {
                    var canPlace = true
                    for (k in 0 until size) { if (missCells[idx + k * 10] || board[idx + k * 10] != -1) { canPlace = false; break } }
                    if (canPlace) valid.add(Placement(idx, true))
                }
            }
            return valid
        }

        fun isValidUniverse(): Boolean {
            for (i in 0 until 100) { if (hitCells[i] && board[i] == -1) return false }
            val shipHitCounts = IntArray(5) { 0 }
            val shipHasSunkPeg = BooleanArray(5) { false }

            for (i in 0 until 100) {
                val sIdx = board[i]
                if (sIdx != -1 && hitCells[i]) {
                    shipHitCounts[sIdx]++
                    if (sunkCells[i]) shipHasSunkPeg[sIdx] = true
                }
            }

            for (sIdx in 0 until 5) {
                val size = shipSizes[sIdx]
                val hits = shipHitCounts[sIdx]
                val hasSunk = shipHasSunkPeg[sIdx]
                if (hasSunk && hits != size) return false
                if (hits == size && !hasSunk) return false
            }
            return true
        }

        fun solve(depth: Int, currentOrder: List<Int>): Boolean {
            if (System.currentTimeMillis() - startTime > 1500) return false

            var uncoveredHits = 0
            for (i in 0 until 100) { if (hitCells[i] && board[i] == -1) uncoveredHits++ }
            var remainingCapacity = 0
            for (i in depth until 5) remainingCapacity += shipSizes[currentOrder[i]]

            if (uncoveredHits > remainingCapacity) return false

            if (depth == 5) {
                if (isValidUniverse()) {
                    validUniverses++
                    for (i in 0 until 100) { if (board[i] != -1 && !hitCells[i] && !missCells[i]) heatMap[i]++ }
                    return true
                }
                return false
            }

            val shipIndex = currentOrder[depth]
            val size = shipSizes[shipIndex]
            val placements = getDynamicPlacements(size).shuffled().sortedByDescending { p ->
                var hitsCovered = 0
                for (k in 0 until size) {
                    val idx = p.startIdx + if (p.isVertical) k * 10 else k
                    if (hitCells[idx]) hitsCovered++
                }
                hitsCovered
            }

            for (p in placements) {
                if (System.currentTimeMillis() - startTime > 1500) return false
                for (k in 0 until size) { board[p.startIdx + if (p.isVertical) k * 10 else k] = shipIndex }
                val foundValidUniverse = solve(depth + 1, currentOrder)
                for (k in 0 until size) { board[p.startIdx + if (p.isVertical) k * 10 else k] = -1 }
                if (foundValidUniverse) return true
            }
            return false
        }

        val useParity = (totalHits == 0)

        if (totalHits > 0) {
            val baseOrder = listOf(0, 1, 2, 3, 4)
            while (validUniverses < targetSimulations && System.currentTimeMillis() - startTime < 1500) {
                if (!solve(0, baseOrder.shuffled())) break
            }
        }

        if (validUniverses < 10 && totalHits > 0) {
            val densityBot = DensityBot()
            val fallbackMap = densityBot.getRawDensityMap(moves)
            return BotDecision(densityBot.getBestMove(moves), "Density Fallback (Math Trap: Only $validUniverses universes)", fallbackMap)
        }

        var bestIdx = 0
        var maxHeat = -1

        for (i in 0 until 100) {
            if (!missCells[i] && !hitCells[i]) {
                val x = i % 10
                val y = i / 10

                val heat = if (useParity) {
                    var domainHeat = 0
                    for (sIdx in 0 until 5) {
                        val placements = getDynamicPlacements(shipSizes[sIdx])
                        for (p in placements) {
                            for(k in 0 until shipSizes[sIdx]) {
                                if ((p.startIdx + if(p.isVertical) k*10 else k) == i) domainHeat++
                            }
                        }
                    }
                    heatMap[i] = domainHeat
                    domainHeat
                } else heatMap[i]

                if (useParity && (x + y) % 2 != 0) continue

                if (heat > maxHeat) {
                    maxHeat = heat
                    bestIdx = i
                }
            }
        }

        if (maxHeat <= 0 && useParity) {
            for (i in 0 until 100) {
                if (!missCells[i] && !hitCells[i]) {
                    var domainHeat = 0
                    for (sIdx in 0 until 5) {
                        val placements = getDynamicPlacements(shipSizes[sIdx])
                        for (p in placements) {
                            for(k in 0 until shipSizes[sIdx]) {
                                if ((p.startIdx + if(p.isVertical) k*10 else k) == i) domainHeat++
                            }
                        }
                    }
                    heatMap[i] = domainHeat
                    if (domainHeat > maxHeat) {
                        maxHeat = domainHeat
                        bestIdx = i
                    }
                }
            }
        }

        val visualHeatmap = Array(10) { IntArray(10) }
        for (i in 0 until 100) visualHeatmap[i % 10][i / 10] = heatMap[i]

        if (maxHeat <= 0) {
            val densityBot = DensityBot()
            val fallbackMap = densityBot.getRawDensityMap(moves)
            return BotDecision(densityBot.getBestMove(moves), "Density Fallback (Cold Map)", fallbackMap)
        }

        val logMode = if (useParity) "Endgame Solver (Parity Hunt)" else "Endgame Solver ($validUniverses Universes)"
        return BotDecision(Pair(bestIdx % 10, bestIdx / 10), logMode, visualHeatmap)
    }

    fun getLiveHeatmap(moves: List<Move>): Array<IntArray>? {
        return getBestMove(moves).heatMap
    }
}