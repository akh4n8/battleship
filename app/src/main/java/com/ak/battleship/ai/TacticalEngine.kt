package com.ak.battleship.ai

import android.content.Context
import com.ak.battleship.data.Game
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
        gameId: Int,
        adlerOffensivePrior: Array<FloatArray>? = null,
        moriartyPrior: Array<FloatArray>? = null
    ): BotDecision {
        return withContext(Dispatchers.Default) {
            when {
                opponentName.contains("Adler", ignoreCase = true) -> AdlerBot.getBestMove(botMovesSoFar, adlerOffensivePrior, playerName, gameId)
                opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getBestMove(botMovesSoFar, gameId)
                opponentName.contains("Watson", ignoreCase = true) -> WatsonBot.getBestMove(botMovesSoFar)

                // THE FIX: Explicitly route Moriarty to Mycroft's MCMC engine!
                opponentName.contains("Mycroft", ignoreCase = true) || opponentName.contains("Moriarty", ignoreCase = true) ->
                    MycroftBot.getBestMove(botMovesSoFar, gameId, moriartyPrior, isMoriarty = opponentName.contains("Moriarty", ignoreCase = true))

                else -> {
                    val densityBot = WatsonBot
                    densityBot.getBestMove(botMovesSoFar).copy(
                        log = "Standard Density Map",
                        heatMap = densityBot.getLiveHeatmap(botMovesSoFar)
                    )
                }
            }
        }
    }

    fun generateBotFleet(opponentName: String, adlerDefensivePrior: Array<FloatArray>? = null): List<Ship> {
        return when {
            // Adler keeps her rigid, exploitable pure-Bayesian algorithm
            opponentName.contains("Adler", ignoreCase = true) -> AdlerBot.generatePhantomFleet(adlerDefensivePrior)
            // Moriarty gets the new Entropy-Maximized logic (ready for an ML prior!)
            opponentName.contains("Moriarty", ignoreCase = true) -> MoriartyBot.generatePhantomFleet(adlerDefensivePrior)
            else -> generateRandomBotFleet()
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
            opponentName.contains("Adler", ignoreCase = true) -> AdlerBot.getLiveDiagnostics(moves)
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveDiagnostics(moves)
            else -> null
        }
    }

    fun getLiveLivingFleet(opponentName: String, moves: List<Move>): List<Int>? {
        return when {
            opponentName.contains("Adler", ignoreCase = true) -> AdlerBot.getLiveLivingFleet(moves)
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveLivingFleet(moves)
            else -> null
        }
    }

    fun getLiveHeatmap(
        opponentName: String,
        moves: List<Move>,
        context: Context,
        gameId: Int,
        adlerOffensivePrior: Array<FloatArray>? = null,
        moriartyPrior: Array<FloatArray>? = null
    ): Array<IntArray>? {
        return when {
            opponentName.contains("Adler", ignoreCase = true) -> AdlerBot.getLiveHeatmap(moves, gameId, adlerOffensivePrior)
            opponentName.contains("Sherlock", ignoreCase = true) -> SherlockBot.getLiveHeatmap(moves, gameId)
            opponentName.contains("Watson", ignoreCase = true) -> WatsonBot.getLiveHeatmap(moves)

            // THE FIX: Route Moriarty's Heatmap to Mycroft too!
            opponentName.contains("Mycroft", ignoreCase = true) || opponentName.contains("Moriarty", ignoreCase = true) ->
                MycroftBot.getLiveHeatmap(moves, gameId, moriartyPrior)

            else -> WatsonBot.getLiveHeatmap(moves)
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

    fun executeLinearKill(
        board: Array<IntArray>,
        activeHits: List<Pair<Int, Int>>,
        claimedHits: Set<Pair<Int, Int>>,
        random: kotlin.random.Random
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
                if (validTargets.isNotEmpty()) return validTargets.random(random)

            } else if (ys.size == 1) {
                val y = ys.first()
                val endpoints = mutableListOf<Pair<Int, Int>>()

                var xMin = xs.minOrNull() ?: 0
                while (xMin >= 0 && board[xMin][y] == CELL_HIT) xMin--
                if (xMin >= 0 && board[xMin][y] == CELL_UNKNOWN) endpoints.add(Pair(xMin, y))

                var xMax = xs.maxOrNull() ?: 0
                while (xMax <= 9 && board[xMax][y] == CELL_HIT) xMax++
                if (xMax <= 9 && board[xMax][y] == CELL_UNKNOWN) endpoints.add(Pair(xMax, y))

                if (endpoints.isNotEmpty()) return endpoints.random(random)

            } else if (xs.size == 1) {
                val x = xs.first()
                val endpoints = mutableListOf<Pair<Int, Int>>()

                var yMin = ys.minOrNull() ?: 0
                while (yMin >= 0 && board[x][yMin] == CELL_HIT) yMin--
                if (yMin >= 0 && board[x][yMin] == CELL_UNKNOWN) endpoints.add(Pair(x, yMin))

                var yMax = ys.maxOrNull() ?: 0
                while (yMax <= 9 && board[x][yMax] == CELL_HIT) yMax++
                if (yMax <= 9 && board[x][yMax] == CELL_UNKNOWN) endpoints.add(Pair(x, yMax))

                if (endpoints.isNotEmpty()) return endpoints.random(random)

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
                if (validEndpoints.isNotEmpty()) return validEndpoints.random(random)
            }

            for (hit in cluster) {
                val validTargets = getUnknownNeighbors(board, hit)
                if (validTargets.isNotEmpty()) return validTargets.random(random)
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
// BOT 2: ADLER (EXPLOITATIVE BAYESIAN)
// ==========================================

private object AdlerBot {

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
        val matrixTypeTag = if (isUniformFallback) "Fallback (Uniform 1.0)" else "adler_offense_$playerName"

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

        for ((size, name) in shipSpecs) {
            val validPlacements = mutableListOf<Triple<Int, Int, Boolean>>()
            val heatScores = mutableListOf<Float>()

            for (isVertical in listOf(true, false)) {
                val maxX = if (isVertical) 10 else 10 - size + 1
                val maxY = if (isVertical) 10 - size + 1 else 10

                for (x in 0 until maxX) {
                    for (y in 0 until maxY) {
                        var collision = false
                        var currentHeat = 0.0f

                        for (i in 0 until size) {
                            val cx = x + if (!isVertical) i else 0
                            val cy = y + if (isVertical) i else 0
                            if (grid[cx][cy]) { collision = true; break }
                            currentHeat += safePrior[cx][cy]
                        }

                        if (!collision) {
                            validPlacements.add(Triple(x, y, isVertical))
                            heatScores.add(currentHeat)
                        }
                    }
                }
            }

            if (validPlacements.isNotEmpty()) {
                val chosenIndex = if (kotlin.random.Random.nextFloat() < 0.30f) {
                    validPlacements.indices.random()
                } else {
                    var minHeat = Float.MAX_VALUE
                    val bestIndices = mutableListOf<Int>()

                    for (i in heatScores.indices) {
                        if (heatScores[i] < minHeat) {
                            minHeat = heatScores[i]
                            bestIndices.clear()
                            bestIndices.add(i)
                        } else if (heatScores[i] == minHeat) {
                            bestIndices.add(i)
                        }
                    }
                    bestIndices.random()
                }

                val (px, py, bestIsVertical) = validPlacements[chosenIndex]

                for (i in 0 until size) {
                    val cx = px + if (!bestIsVertical) i else 0
                    val cy = py + if (bestIsVertical) i else 0
                    grid[cx][cy] = true
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
// BOT 3: WATSON (GEOMETRIC MATH)
// ==========================================

private object WatsonBot {
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
        return BotDecision(target, "Watson: Geometric Math", densityMap)
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
                        var overlapHits = 0
                        for (i in 0 until size) {
                            if (board[x + i][y] == CELL_MISS) { canFit = false; break }
                            if (board[x + i][y] == CELL_HIT) overlapHits++
                        }
                        if (canFit) {
                            val weight = if (overlapHits > 0) 1 + overlapHits else 1
                            for (i in 0 until size) {
                                if (board[x + i][y] == CELL_UNKNOWN) densityMap[x + i][y] += weight
                            }
                        }
                    }
                    if (y + size <= 10) {
                        var canFit = true
                        var overlapHits = 0
                        for (i in 0 until size) {
                            if (board[x][y + i] == CELL_MISS) { canFit = false; break }
                            if (board[x][y + i] == CELL_HIT) overlapHits++
                        }
                        if (canFit) {
                            val weight = if (overlapHits > 0) 1 + overlapHits else 1
                            for (i in 0 until size) {
                                if (board[x][y + i] == CELL_UNKNOWN) densityMap[x][y + i] += weight
                            }
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
// BOT 6: MYCROFT (MCMC JOINT-PROBABILITY ENGINE)
// ==========================================

private object MycroftBot {

    // Now actively accepts the Neural Network Prior to skew the MCMC targeting
    fun getBestMove(moves: List<Move>, gameId: Int, moriartyPrior: Array<FloatArray>? = null, isMoriarty: Boolean = false): BotDecision {
        val random = kotlin.random.Random((gameId * 10000) + moves.size)

        val board = DeductionEngine.getBoardState(moves)
        val analysis = DeductionEngine.analyzeBoardState(board, moves)
        val diagnosticMap = Array(10) { IntArray(10) { 0 } }
        val heatMap = Array(10) { IntArray(10) { 0 } }

        // Populate diagnostic visualization layers
        analysis.claimedHits.forEach { diagnosticMap[it.first][it.second] = 1 }
        analysis.activeHits.forEach { diagnosticMap[it.first][it.second] = 3 }

        // --- PHASE 1: THE ASSASSIN (Deduction Override) ---
        if (analysis.activeHits.isNotEmpty()) {
            val target = DeductionEngine.executeLinearKill(board, analysis.activeHits, analysis.claimedHits.toSet(), random)
            if (target != null) {
                heatMap[target.first][target.second] = 100
                return BotDecision(
                    target,
                    "Mycroft (Assassin Mode):\nTracing active vector.", // <-- ADDED \n
                    heatMap,
                    diagnosticMap
                )
            }
        }

        // --- PHASE 2: 128-BIT BITBOARD EXTRACTION & CHRONOLOGY ---
        var missLow = 0L;
        var missHigh = 0L
        var allHitsLow = 0L;
        var allHitsHigh = 0L
        var sunkLow = 0L;
        var sunkHigh = 0L
        var numSunks = 0
        val pegTimes = IntArray(100) { -1 }

        moves.filter { !it.isOffense && it.result != "MISS" }.forEach {
            pegTimes[it.y * 10 + it.x] = it.shotNumber
        }

        for (x in 0..9) {
            for (y in 0..9) {
                val idx = y * 10 + x
                val isHigh = idx >= 64
                val shift = if (isHigh) idx - 64 else idx
                val bit = 1L shl shift

                when (board[x][y]) {
                    DeductionEngine.CELL_MISS -> if (isHigh) missHigh =
                        missHigh or bit else missLow = missLow or bit

                    DeductionEngine.CELL_HIT -> if (isHigh) allHitsHigh =
                        allHitsHigh or bit else allHitsLow = allHitsLow or bit

                    DeductionEngine.CELL_SUNK -> {
                        if (isHigh) {
                            allHitsHigh = allHitsHigh or bit; sunkHigh = sunkHigh or bit
                        } else {
                            allHitsLow = allHitsLow or bit; sunkLow = sunkLow or bit
                        }
                        numSunks++
                    }
                }
            }
        }

        // --- PHASE 3: STATIC PLACEMENT PRECOMPUTATION ---
        val fleetSizes = intArrayOf(5, 4, 3, 3, 2)
        val placementsLow = Array(5) { LongArray(200) }
        val placementsHigh = Array(5) { LongArray(200) }
        val placementCounts = IntArray(5) { 0 }

        for (shipIdx in 0..4) {
            val size = fleetSizes[shipIdx]
            var count = 0
            for (x in 0..9) {
                for (y in 0..9) {
                    // Horizontal Placements
                    if (x + size <= 10) {
                        var pLow = 0L;
                        var pHigh = 0L;
                        var valid = true
                        for (i in 0 until size) {
                            val idx = y * 10 + (x + i)
                            if (idx < 64) {
                                if ((missLow and (1L shl idx)) != 0L) {
                                    valid = false; break
                                }
                                pLow = pLow or (1L shl idx)
                            } else {
                                if ((missHigh and (1L shl (idx - 64))) != 0L) {
                                    valid = false; break
                                }
                                pHigh = pHigh or (1L shl (idx - 64))
                            }
                        }
                        if (valid) {
                            placementsLow[shipIdx][count] = pLow; placementsHigh[shipIdx][count] =
                                pHigh; count++
                        }
                    }
                    // Vertical Placements
                    if (y + size <= 10) {
                        var pLow = 0L;
                        var pHigh = 0L;
                        var valid = true
                        for (i in 0 until size) {
                            val idx = (y + i) * 10 + x
                            if (idx < 64) {
                                if ((missLow and (1L shl idx)) != 0L) {
                                    valid = false; break
                                }
                                pLow = pLow or (1L shl idx)
                            } else {
                                if ((missHigh and (1L shl (idx - 64))) != 0L) {
                                    valid = false; break
                                }
                                pHigh = pHigh or (1L shl (idx - 64))
                            }
                        }
                        if (valid) {
                            placementsLow[shipIdx][count] = pLow; placementsHigh[shipIdx][count] =
                                pHigh; count++
                        }
                    }
                }
            }
            placementCounts[shipIdx] = count
            if (count == 0) return BotDecision(Pair(0, 0), "Error: No valid space", heatMap)
        }

        // --- PHASE 4: MCMC SEED INITIATION (CSP Bridging) ---
        val stateLow = LongArray(5) { 0L }
        val stateHigh = LongArray(5) { 0L }
        val unplacedIndices = mutableListOf(0, 1, 2, 3, 4)

        for (ship in analysis.deadShips) {
            val size = ship.size
            val targetIdx = unplacedIndices.firstOrNull { fleetSizes[it] == size }
            if (targetIdx != null) {
                unplacedIndices.remove(targetIdx)
                var pLow = 0L;
                var pHigh = 0L
                for ((px, py) in ship) {
                    val idx = py * 10 + px
                    if (idx < 64) pLow = pLow or (1L shl idx) else pHigh =
                        pHigh or (1L shl (idx - 64))
                }
                stateLow[targetIdx] = pLow
                stateHigh[targetIdx] = pHigh
            }
        }

        for (idx in unplacedIndices) {
            var placed = false
            for (attempt in 0..100) {
                val randChoice = random.nextInt(placementCounts[idx])
                val pLow = placementsLow[idx][randChoice]
                val pHigh = placementsHigh[idx][randChoice]

                var collision = false
                for (i in 0..4) {
                    if (i != idx && ((stateLow[i] and pLow) != 0L || (stateHigh[i] and pHigh) != 0L)) {
                        collision = true; break
                    }
                }
                if (!collision && (pLow and allHitsLow) == 0L && (pHigh and allHitsHigh) == 0L) {
                    stateLow[idx] = pLow; stateHigh[idx] = pHigh
                    placed = true; break
                }
            }
            if (!placed) {
                val fallback = SherlockBot.getBestMove(moves, gameId)
                return fallback.copy(log = "Mycroft: MCMC Seed Failed.\nDeferring to Sherlock CSP.") // <-- ADDED \n
            }
        }

        // --- PHASE 5: THE ZERO-ALLOCATION MARKOV CHAIN ---
        val rawHeatmap = IntArray(100) { 0 }
        var validUniverses = 0
        val maxTimeMs = 3000L
        val startTime = System.currentTimeMillis()
        val burnInPeriod = 1000

        for (step in 0 until 100_000) {
            if (step % 1000 == 0 && System.currentTimeMillis() - startTime > maxTimeMs) break

            val jumpTwo = random.nextFloat() < 0.2f
            val idx1 = random.nextInt(5)
            var idx2 = -1
            if (jumpTwo) {
                idx2 = random.nextInt(5)
                while (idx2 == idx1) idx2 = random.nextInt(5)
            }

            val oldLow1 = stateLow[idx1];
            val oldHigh1 = stateHigh[idx1]
            val oldLow2 = if (jumpTwo) stateLow[idx2] else 0L
            val oldHigh2 = if (jumpTwo) stateHigh[idx2] else 0L

            val choice1 = random.nextInt(placementCounts[idx1])
            val newLow1 = placementsLow[idx1][choice1]
            val newHigh1 = placementsHigh[idx1][choice1]

            var newLow2 = 0L;
            var newHigh2 = 0L
            if (jumpTwo) {
                val choice2 = random.nextInt(placementCounts[idx2])
                newLow2 = placementsLow[idx2][choice2]
                newHigh2 = placementsHigh[idx2][choice2]
            }

            var validProposal = true
            if (jumpTwo && ((newLow1 and newLow2) != 0L || (newHigh1 and newHigh2) != 0L)) validProposal =
                false

            if (validProposal) {
                var otherLow = 0L;
                var otherHigh = 0L
                for (i in 0..4) {
                    if (i != idx1 && i != idx2) {
                        otherLow = otherLow or stateLow[i]; otherHigh = otherHigh or stateHigh[i]
                    }
                }
                if ((newLow1 and otherLow) != 0L || (newHigh1 and otherHigh) != 0L) validProposal =
                    false
                if (jumpTwo && ((newLow2 and otherLow) != 0L || (newHigh2 and otherHigh) != 0L)) validProposal =
                    false
            }

            if (validProposal) {
                stateLow[idx1] = newLow1; stateHigh[idx1] = newHigh1
                if (jumpTwo) {
                    stateLow[idx2] = newLow2; stateHigh[idx2] = newHigh2
                }

                val touchesHits =
                    (newLow1 and allHitsLow) != 0L || (newHigh1 and allHitsHigh) != 0L ||
                            (oldLow1 and allHitsLow) != 0L || (oldHigh1 and allHitsHigh) != 0L ||
                            (jumpTwo && ((newLow2 and allHitsLow) != 0L || (newHigh2 and allHitsHigh) != 0L ||
                                    (oldLow2 and allHitsLow) != 0L || (oldHigh2 and allHitsHigh) != 0L))

                if (touchesHits) {
                    var occLow = 0L;
                    var occHigh = 0L
                    for (i in 0..4) {
                        occLow = occLow or stateLow[i]; occHigh = occHigh or stateHigh[i]
                    }

                    if ((occLow and allHitsLow) != allHitsLow || (occHigh and allHitsHigh) != allHitsHigh) {
                        validProposal = false
                    } else {
                        var fullyHitCount = 0
                        var fhSunkLow = 0L;
                        var fhSunkHigh = 0L
                        for (i in 0..4) {
                            val sl = stateLow[i];
                            val sh = stateHigh[i]
                            if ((sl and allHitsLow) == sl && (sh and allHitsHigh) == sh) {
                                fullyHitCount++
                                var maxTime = -1
                                var lethalL = 0L;
                                var lethalH = 0L

                                var tempSl = sl
                                while (tempSl != 0L) {
                                    val lsb = tempSl and -tempSl
                                    val b = java.lang.Long.numberOfTrailingZeros(lsb)
                                    val t = pegTimes[b]
                                    if (t > maxTime) {
                                        maxTime = t; lethalL = lsb; lethalH = 0L
                                    }
                                    tempSl = tempSl xor lsb
                                }

                                var tempSh = sh
                                while (tempSh != 0L) {
                                    val lsb = tempSh and -tempSh
                                    val b = java.lang.Long.numberOfTrailingZeros(lsb)
                                    val t = pegTimes[b + 64]
                                    if (t > maxTime) {
                                        maxTime = t; lethalH = lsb; lethalL = 0L
                                    }
                                    tempSh = tempSh xor lsb
                                }

                                if ((lethalL and sunkLow) == 0L && (lethalH and sunkHigh) == 0L) {
                                    validProposal = false; break
                                }
                                fhSunkLow = fhSunkLow or lethalL; fhSunkHigh = fhSunkHigh or lethalH
                            }
                        }
                        if (validProposal && (fullyHitCount != numSunks || fhSunkLow != sunkLow || fhSunkHigh != sunkHigh)) {
                            validProposal = false
                        }
                    }
                }
            }

            if (!validProposal) {
                stateLow[idx1] = oldLow1; stateHigh[idx1] = oldHigh1
                if (jumpTwo) {
                    stateLow[idx2] = oldLow2; stateHigh[idx2] = oldHigh2
                }
            }

            if (step >= burnInPeriod) {
                validUniverses++
                for (i in 0..4) {
                    var sl = stateLow[i]
                    while (sl != 0L) {
                        val lsb = sl and -sl
                        rawHeatmap[java.lang.Long.numberOfTrailingZeros(lsb)]++
                        sl = sl xor lsb
                    }
                    var sh = stateHigh[i]
                    while (sh != 0L) {
                        val lsb = sh and -sh
                        rawHeatmap[java.lang.Long.numberOfTrailingZeros(lsb) + 64]++
                        sh = sh xor lsb
                    }
                }
            }
        }

        // --- PHASE 6: HEATMAP ASSEMBLY & TARGETING ---
        var maxHeat = -1
        var bestMoves = mutableListOf<Pair<Int, Int>>()

        val parityOffset = gameId % 2

        for (x in 0..9) {
            for (y in 0..9) {
                if (board[x][y] == DeductionEngine.CELL_UNKNOWN) {
                    val idx = y * 10 + x
                    val baseHeat = rawHeatmap[idx].toFloat()
                    val finalHeat: Int

                    val isValidParity = (x + y) % 2 == parityOffset

                    if (moriartyPrior != null) {
                        if (isValidParity && baseHeat > 0) {
                            var localMaxBias = moriartyPrior[x][y]

                            // Spatial Diffusion: Absorb peaks from off-parity neighbors
                            val neighbors = listOf(Pair(x - 1, y), Pair(x + 1, y), Pair(x, y - 1), Pair(x, y + 1))
                            for ((nx, ny) in neighbors) {
                                if (nx in 0..9 && ny in 0..9) {
                                    val neighborBias = moriartyPrior[nx][ny]
                                    if (neighborBias > localMaxBias) {
                                        localMaxBias = neighborBias
                                    }
                                }
                            }
                            
                            // MCMC BINARY MASK: MCMC confirms a ship can fit here. 
                            // We throw away its center-biased magnitude and let ML take 100% control.
                            finalHeat = (localMaxBias * 10000).toInt()
                        } else {
                            // Impossible island or off-parity
                            finalHeat = 0
                        }
                    } else {
                        // Mycroft Maverick Mode: No psychology, pure math
                        if (isMoriarty && !isValidParity) {
                            finalHeat = 0
                        } else {
                            finalHeat = baseHeat.toInt()
                        }
                    }

                    heatMap[x][y] = finalHeat

                    if (finalHeat > maxHeat) {
                        maxHeat = finalHeat
                        bestMoves = mutableListOf(Pair(x, y))
                    } else if (finalHeat == maxHeat && finalHeat > 0) {
                        bestMoves.add(Pair(x, y))
                    }
                }
            }
        }

        val target = if (maxHeat > 0 && bestMoves.isNotEmpty()) {
            bestMoves.random(random) // Deterministic tie-breaking is strictly preserved
        } else {
            val openWater = mutableListOf<Pair<Int, Int>>()
            for (x in 0..9) for (y in 0..9) if (board[x][y] == DeductionEngine.CELL_UNKNOWN) openWater.add(
                Pair(x, y)
            )
            if (openWater.isNotEmpty()) openWater.random(random) else Pair(0, 0)
        }

        val logMessage = when {
            isMoriarty && moriartyPrior != null -> "Moriarty Neural Offense:\nML Prior scaled $validUniverses MCMC universes."
            isMoriarty && moriartyPrior == null -> "Moriarty (Maverick Fallback):\nLow ML confidence ratio or <5 history games.\nFallback to Mycroft MCMC ($validUniverses universes)."
            moriartyPrior != null -> "Mycroft (Moriarty Guided):\nML Prior scaled $validUniverses MCMC universes."
            else -> "Mycroft (Maverick Mode):\nSampled $validUniverses universes."
        }

        return BotDecision(
            target,
            logMessage,
            heatMap,
            diagnosticMap
        )
    }

    fun getLiveHeatmap(moves: List<Move>, gameId: Int, moriartyPrior: Array<FloatArray>? = null): Array<IntArray>? {
        return getBestMove(moves, gameId, moriartyPrior).heatMap
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
}

// ==========================================
// BOT 7: MORIARTY (NEURAL NETWORK SEQUENCE PREDICTOR)
// ==========================================

/**
 * AI LAYER: Moriarty (The Neural Network)
 * * Non-Stationary Sequence Predictor.
 * * Uses an LSTM/Transformer TFLite model to predict human placement meta-patterns.
 * * Now cleanly exposed to the ViewModel for execution prior to tactical routing.
 */
object MoriartyBot {

    private var cachedDefenseBuffer: java.nio.MappedByteBuffer? = null
    private var cachedOffenseBuffer: java.nio.MappedByteBuffer? = null

    private fun getModelBuffer(context: Context, isOffense: Boolean): java.nio.MappedByteBuffer {
        val fileName = if (isOffense) "moriarty_offense.tflite" else "moriarty_defense.tflite"
        if (isOffense) {
            if (cachedOffenseBuffer == null) {
                val fd = context.assets.openFd(fileName)
                java.io.FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    cachedOffenseBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
            return cachedOffenseBuffer!!
        } else {
            if (cachedDefenseBuffer == null) {
                val fd = context.assets.openFd(fileName)
                java.io.FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    cachedDefenseBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
            return cachedDefenseBuffer!!
        }
    }

    private fun getShipWeight(shipClass: String): Float {
        return when (shipClass) {
            "Carrier" -> 0.4f
            "Battleship" -> 0.6f
            "Cruiser", "Submarine" -> 0.8f
            "Destroyer" -> 1.0f
            else -> 0.7f
        }
    }

    private fun buildMultiChannelTensor(
        targetPlayer: String,
        allGames: List<Game>,
        allMoves: List<Move>,
        liveMoves: List<Move>?,
        limitTimestamp: Long?
    ): FloatArray? {
        val validGames = allGames.filter {
            (it.playerName.equals(targetPlayer, ignoreCase = true) || it.opponentName.equals(targetPlayer, ignoreCase = true)) &&
                    it.result != null &&
                    (limitTimestamp == null || it.timestamp < limitTimestamp)
        }.sortedBy { it.timestamp }

        if (validGames.size < 5) return null
        val historySlice = validGames.takeLast(5)
        val movesByGameId = allMoves.groupBy { it.gameId }

        val stepsCount = if (liveMoves != null) 6 else 5
        val flatTensor = FloatArray(stepsCount * 7 * 10 * 10)
        var tensorIdx = 0

        var cumulativeTraumaMisses = Array(10) { FloatArray(10) { 0.0f } }
        var cumulativeTraumaHits = Array(10) { FloatArray(10) { 0.0f } }

        fun bakeChannels(gameMoves: List<Move>, traumaMatrix: Array<FloatArray>, traumaHitsMatrix: Array<FloatArray>, isLiveStep: Boolean) {
            val playerIsP1 = if (isLiveStep) true else (gameMoves.firstOrNull()?.let { m ->
                val g = validGames.find { it.id == m.gameId }
                g?.playerName.equals(targetPlayer, ignoreCase = true)
            } ?: true)

            // THE FIX: Use the isOffense boolean mapped against the player's seat!
            val humanShots = gameMoves.filter { m ->
                (m.isOffense == playerIsP1) && !isShipData(m.result)
            }.sortedBy { it.shotNumber }

            val humanShips = gameMoves.filter { m ->
                (m.isOffense != playerIsP1) && isShipData(m.result)
            }

            val chHumanMisses = Array(10) { FloatArray(10) { 0.0f } }
            val chHumanHits = Array(10) { FloatArray(10) { 0.0f } }
            val chClustering = Array(10) { FloatArray(10) { 0.0f } }
            val chChronology = Array(10) { FloatArray(10) { 0.0f } }
            val chSpatial = Array(10) { FloatArray(10) { 0.0f } }

            val totalShots = humanShots.size
            val hitQueue = mutableListOf<Pair<Int, Int>>()

            humanShots.forEachIndexed { index, move ->
                if (move.x in 0..9 && move.y in 0..9) {
                    if (totalShots > 1) {
                        chChronology[move.x][move.y] = index.toFloat() / (totalShots - 1).toFloat()
                    }
                    val isHit = move.result == "HIT" || move.isSunk
                    if (isHit) {
                        chHumanHits[move.x][move.y] = 1.0f
                        chClustering[move.x][move.y] = 1.0f
                        hitQueue.add(Pair(move.x, move.y))
                    } else {
                        chHumanMisses[move.x][move.y] = 1.0f
                        for ((hx, hy) in hitQueue) {
                            if (Math.abs(hx - move.x) <= 1 && Math.abs(hy - move.y) <= 1) {
                                chClustering[move.x][move.y] = 0.5f
                                break
                            }
                        }
                    }
                }
            }

            if (!isLiveStep) {
                humanShips.forEach { move ->
                    if (move.x in 0..9 && move.y in 0..9) {
                        chSpatial[move.x][move.y] = getShipWeight(move.result)
                    }
                }
            }

            for (ch in 0..6) {
                for (y in 0..9) {
                    for (x in 0..9) {
                        flatTensor[tensorIdx++] = when (ch) {
                            0 -> traumaMatrix[x][y]
                            1 -> traumaHitsMatrix[x][y]
                            2 -> chHumanMisses[x][y]
                            3 -> chHumanHits[x][y]
                            4 -> chClustering[x][y]
                            5 -> chChronology[x][y]
                            else -> chSpatial[x][y]
                        }
                    }
                }
            }
        }

        for (game in historySlice) {
            val gameMoves = movesByGameId[game.id] ?: emptyList()
            bakeChannels(gameMoves, cumulativeTraumaMisses, cumulativeTraumaHits, isLiveStep = false)

            val playerIsP1 = game.playerName.equals(targetPlayer, ignoreCase = true)
            val botShots = gameMoves.filter { m ->
                (m.isOffense != playerIsP1) && !isShipData(m.result)
            }
            val humanShips = gameMoves.filter { m ->
                (m.isOffense != playerIsP1) && isShipData(m.result)
            }

            val currentBotMisses = Array(10) { FloatArray(10) { 0.0f } }
            val currentBotHits = Array(10) { FloatArray(10) { 0.0f } }
            
            botShots.forEach { move ->
                if (move.x in 0..9 && move.y in 0..9) {
                    val isHit = move.result == "HIT" || move.isSunk
                    if (isHit) {
                        val shipHit = humanShips.find { it.x == move.x && it.y == move.y }
                        val weight = if (shipHit != null) getShipWeight(shipHit.result) else 0.7f
                        currentBotHits[move.x][move.y] += weight
                    } else {
                        currentBotMisses[move.x][move.y] += 1.0f
                    }
                }
            }

            for (x in 0..9) {
                for (y in 0..9) {
                    cumulativeTraumaMisses[x][y] = (cumulativeTraumaMisses[x][y] * 0.85f) + currentBotMisses[x][y]
                    cumulativeTraumaHits[x][y] = (cumulativeTraumaHits[x][y] * 0.85f) + currentBotHits[x][y]
                }
            }
        }

        if (liveMoves != null) {
            bakeChannels(liveMoves, cumulativeTraumaMisses, cumulativeTraumaHits, isLiveStep = true)
        }

        return flatTensor
    }

    fun getPsychologicalPrior(
        context: Context,
        playerName: String,
        allGames: List<Game>,
        allMoves: List<Move>,
        liveMoves: List<Move>? = null,
        limitTimestamp: Long? = null
    ): Array<FloatArray>? {
        val isOffense = (liveMoves != null)
        val flatData = buildMultiChannelTensor(playerName, allGames, allMoves, liveMoves, limitTimestamp) ?: return null

        val steps = if (isOffense) 6 else 5
        val totalBytes = steps * 7 * 10 * 10 * 4

        val inputBuffer = java.nio.ByteBuffer.allocateDirect(totalBytes).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }
        flatData.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val outputBuffer = Array(1) { FloatArray(100) }
        val interpreter = org.tensorflow.lite.Interpreter(getModelBuffer(context, isOffense))
        interpreter.run(inputBuffer, outputBuffer)
        interpreter.close()

        val logits = outputBuffer[0]
        val probabilities = FloatArray(100) { i -> (1.0 / (1.0 + Math.exp(-logits[i].toDouble()))).toFloat() }
        val averageProb = probabilities.average().toFloat()
        val maxProb = probabilities.maxOrNull() ?: 1.0f

        val confidenceRatio = if (averageProb > 0f) maxProb / averageProb else 1.0f
        if (confidenceRatio < 1.3f) {
            return null // Fallback to Mycroft
        }

        val priorMap = Array(10) { FloatArray(10) { 1.0f } }
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                val flatIndex = x * 10 + y
                priorMap[x][y] = if (averageProb > 0f) probabilities[flatIndex] / averageProb else 1.0f
            }
        }
        return priorMap
    }

    fun generatePhantomFleet(defensivePrior: Array<FloatArray>?): List<Ship> {
        val safePrior = defensivePrior ?: Array(10) { FloatArray(10) { 1.0f } }
        val shipSpecs = listOf(Pair(5, "Carrier"), Pair(4, "Battleship"), Pair(3, "Cruiser"), Pair(3, "Submarine"), Pair(2, "Destroyer"))
        val fleet = mutableListOf<Ship>()
        val grid = Array(10) { BooleanArray(10) { false } }

        for ((size, name) in shipSpecs) {
            val validPlacements = mutableListOf<Triple<Int, Int, Boolean>>()
            val heatScores = mutableListOf<Float>()

            for (isVertical in listOf(true, false)) {
                val maxX = if (isVertical) 10 else 10 - size + 1
                val maxY = if (isVertical) 10 - size + 1 else 10

                for (x in 0 until maxX) {
                    for (y in 0 until maxY) {
                        var collision = false
                        var currentHeat = 0.0f

                        for (i in 0 until size) {
                            val cx = x + if (!isVertical) i else 0
                            val cy = y + if (isVertical) i else 0
                            if (grid[cx][cy]) { collision = true; break }
                            currentHeat += safePrior[cx][cy]
                        }

                        if (!collision) {
                            validPlacements.add(Triple(x, y, isVertical))
                            heatScores.add(currentHeat)
                        }
                    }
                }
            }

            if (validPlacements.isNotEmpty()) {
                val weights = heatScores.map { Math.exp(-it.toDouble()) }
                val totalWeight = weights.sum()
                var randomVal = Math.random() * totalWeight
                var chosenIndex = validPlacements.indices.last
                for (i in weights.indices) {
                    randomVal -= weights[i]
                    if (randomVal <= 0) {
                        chosenIndex = i
                        break
                    }
                }
                
                val (px, py, bestIsVertical) = validPlacements[chosenIndex]

                for (i in 0 until size) {
                    val cx = px + if (!bestIsVertical) i else 0
                    val cy = py + if (bestIsVertical) i else 0
                    grid[cx][cy] = true
                }
                fleet.add(Ship(name = name, size = size, x = px.toFloat(), y = py.toFloat(), isVertical = bestIsVertical, isPlaced = true))
            }
        }
        return fleet
    }
}