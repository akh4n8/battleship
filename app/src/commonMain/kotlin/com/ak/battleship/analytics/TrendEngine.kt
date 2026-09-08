package com.ak.battleship.analytics

import com.ak.battleship.data.GameRepository
import com.ak.battleship.model.HeatmapData
import com.ak.battleship.model.TrendData
import com.ak.battleship.model.isShipData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * ANALYTICS LAYER: Trend & Statistics Engine
 * * Handles the massive data aggregation required for the Analytics dashboards.
 * * Executes entirely on the IO Dispatcher to prevent UI thread blocking when parsing thousands of moves.
 */
object TrendEngine {

    suspend fun getStats(
        dao: GameRepository,
        playerName: String? = null,
        opponentName: String? = null
    ): Pair<Map<String, String>, Map<String, String>> = withContext(Dispatchers.Default) {

        val allGames = dao.getAllGamesSync()

        val targetGames = allGames.filter { game ->
            val matchesPlayer = playerName == null || playerName == "All" || game.playerName.equals(playerName, ignoreCase = true)
            val matchesOpp = opponentName == null || opponentName == "All" || game.opponentName.equals(opponentName, ignoreCase = true)
            matchesPlayer && matchesOpp
        }

        val targetGameIds = targetGames.map { it.id }
        val moves = if (targetGameIds.isNotEmpty()) dao.getMovesForGames(targetGameIds) else emptyList()

        val completed = targetGames.filter { it.result != null }
        val wins = completed.filter { it.result == "WIN" }
        val losses = completed.filter { it.result == "LOSS" }

        // --- BASIC STATS ---
        val winRate = if (completed.isNotEmpty()) (wins.size.toFloat() * 100 / completed.size).roundToInt() else 0
        val lossRate = if (completed.isNotEmpty()) (losses.size.toFloat() * 100 / completed.size).roundToInt() else 0

        val offenseMoves = moves.filter { it.isOffense && !isShipData(it.result) }
        val hits = offenseMoves.count { it.result == "HIT" }
        val accuracy = if (offenseMoves.isNotEmpty()) (hits.toFloat() * 100 / offenseMoves.size).roundToInt() else 0

        val defenseMoves = moves.filter { !it.isOffense && !isShipData(it.result) }
        val oppHitsCount = defenseMoves.count { it.result == "HIT" }
        val oppAccuracy = if (defenseMoves.isNotEmpty()) (oppHitsCount.toFloat() * 100 / defenseMoves.size).roundToInt() else 0

        val avgTotalShots = if (completed.isNotEmpty()) completed.map { g -> moves.count { it.gameId == g.id && it.isOffense && !isShipData(it.result) } }.average().roundToInt() else 0
        val avgTotalTurns = if (completed.isNotEmpty()) completed.map { g -> moves.filter { it.gameId == g.id && it.isOffense && !isShipData(it.result)}.map { it.turnNumber }.distinct().size }.average().roundToInt() else 0

        val avgShotsToWin = if (wins.isNotEmpty()) wins.map { g -> moves.count { it.gameId == g.id && it.isOffense && !isShipData(it.result) } }.average().roundToInt() else 0
        val avgShotsToLose = if (losses.isNotEmpty()) losses.map { g -> moves.count { it.gameId == g.id && it.isOffense && !isShipData(it.result) } }.average().roundToInt() else 0

        val avgTurnsToWin = if (wins.isNotEmpty()) wins.map { g -> moves.filter { it.gameId == g.id && it.isOffense && !isShipData(it.result)}.map { it.turnNumber }.distinct().size }.average().roundToInt() else 0
        val avgTurnsToLose = if (losses.isNotEmpty()) losses.map { g -> moves.filter { it.gameId == g.id && it.isOffense && !isShipData(it.result)}.map { it.turnNumber }.distinct().size }.average().roundToInt() else 0

        val fastestWinShots = wins.map { g -> moves.count { it.gameId == g.id && it.isOffense && !isShipData(it.result) } }.filter { it > 0 }.minOrNull() ?: 0
        val fastestWinTurns = wins.map { g -> moves.filter { it.gameId == g.id && it.isOffense && !isShipData(it.result)}.map { it.turnNumber }.distinct().size }.filter { it > 0 }.minOrNull() ?: 0

        val fastestLossShots = losses.map { g -> moves.count { it.gameId == g.id && !it.isOffense && !isShipData(it.result) } }.filter { it > 0 }.minOrNull() ?: 0
        val fastestLossTurns = losses.map { g -> moves.filter { it.gameId == g.id && !it.isOffense && !isShipData(it.result)}.map { it.turnNumber }.distinct().size }.filter { it > 0 }.minOrNull() ?: 0

        // --- WILD STATS ENGINE ---
        var destroyerSunkFirstGames = 0
        var destroyerSunkFirstWins = 0
        var oppDestroyerSunkFirstGames = 0
        var oppDestroyerSunkFirstLosses = 0

        var firstBloodGames = 0
        var firstBloodWins = 0
        var firstBloodLosses = 0

        var oppFirstBloodGames = 0
        var oppFirstBloodLosses = 0
        var oppFirstBloodWins = 0

        var flawlessWins = 0
        var flawlessLosses = 0
        var clutchWins = 0
        var clutchLosses = 0
        var ultraClutchWins = 0
        var ultraClutchLosses = 0

        var firstShotAttempts = 0
        var firstShotHits = 0
        var oppFirstShotAttempts = 0
        var oppFirstShotHits = 0

        completed.forEach { game ->
            val gameMoves = moves.filter { it.gameId == game.id }
            val combatMoves = gameMoves.filter { !isShipData(it.result) }.sortedWith(compareBy({ it.turnNumber }, { it.shotNumber }))

            val firstHit = combatMoves.find { it.result == "HIT" }
            if (firstHit != null) {
                if (firstHit.isOffense) {
                    firstBloodGames++
                    if (game.result == "WIN") firstBloodWins++
                    else if (game.result == "LOSS") firstBloodLosses++
                } else {
                    oppFirstBloodGames++
                    if (game.result == "LOSS") oppFirstBloodLosses++
                    else if (game.result == "WIN") oppFirstBloodWins++
                }
            }

            val firstShot = combatMoves.find { it.isOffense }
            if (firstShot != null) {
                firstShotAttempts++
                if (firstShot.result == "HIT" || firstShot.isSunk) firstShotHits++
            }
            val oppFirstShot = combatMoves.find { !it.isOffense }
            if (oppFirstShot != null) {
                oppFirstShotAttempts++
                if (oppFirstShot.result == "HIT" || oppFirstShot.isSunk) oppFirstShotHits++
            }

            val mySunkShips = combatMoves.count { !it.isOffense && it.isSunk }
            val oppSunkShips = combatMoves.count { it.isOffense && it.isSunk }
            val myHits = combatMoves.count { it.isOffense && it.result == "HIT" }
            val oppHits = combatMoves.count { !it.isOffense && it.result == "HIT" }

            if (game.result == "WIN") {
                if (mySunkShips == 0) flawlessWins++
                if (mySunkShips == 4) clutchWins++
                if (oppHits == 16) ultraClutchWins++
            } else if (game.result == "LOSS") {
                if (oppSunkShips == 0) flawlessLosses++
                if (oppSunkShips == 4) clutchLosses++
                if (myHits == 16) ultraClutchLosses++
            }

            val myDestroyerCoords = gameMoves.filter { !it.isOffense && it.result == "Destroyer" }.map { Pair(it.x, it.y) }
            val oppDestroyerCoords = gameMoves.filter { it.isOffense && it.result == "Destroyer" }.map { Pair(it.x, it.y) }

            val myDestroyerSunkMove = combatMoves.find { !it.isOffense && it.isSunk && Pair(it.x, it.y) in myDestroyerCoords }
            val oppDestroyerSunkMove = combatMoves.find { it.isOffense && it.isSunk && Pair(it.x, it.y) in oppDestroyerCoords }

            if (myDestroyerSunkMove != null) {
                val mySunkTime = myDestroyerSunkMove.turnNumber * 1000 + myDestroyerSunkMove.shotNumber
                val oppSunkTime = if (oppDestroyerSunkMove != null) oppDestroyerSunkMove.turnNumber * 1000 + oppDestroyerSunkMove.shotNumber else Int.MAX_VALUE
                if (mySunkTime < oppSunkTime) {
                    destroyerSunkFirstGames++
                    if (game.result == "WIN") destroyerSunkFirstWins++
                }
            }
            if (oppDestroyerSunkMove != null) {
                val oppSunkTime = oppDestroyerSunkMove.turnNumber * 1000 + oppDestroyerSunkMove.shotNumber
                val mySunkTime = if (myDestroyerSunkMove != null) myDestroyerSunkMove.turnNumber * 1000 + myDestroyerSunkMove.shotNumber else Int.MAX_VALUE
                if (oppSunkTime < mySunkTime) {
                    oppDestroyerSunkFirstGames++
                    if (game.result == "LOSS") oppDestroyerSunkFirstLosses++
                }
            }
        }

        val firstBloodPct = if (firstBloodGames > 0) "${(firstBloodWins.toFloat() * 100 / firstBloodGames).roundToInt()}%" else "N/A"
        val oppFirstBloodPct = if (oppFirstBloodGames > 0) "${(oppFirstBloodLosses.toFloat() * 100 / oppFirstBloodGames).roundToInt()}%" else "N/A"
        val myStealBackPct = if (oppFirstBloodGames > 0) "${(oppFirstBloodWins.toFloat() * 100 / oppFirstBloodGames).roundToInt()}%" else "N/A"
        val oppStealBackPct = if (firstBloodGames > 0) "${(firstBloodLosses.toFloat() * 100 / firstBloodGames).roundToInt()}%" else "N/A"
        val destroyerComebackPct = if (destroyerSunkFirstGames > 0) "${(destroyerSunkFirstWins.toFloat() * 100 / destroyerSunkFirstGames).roundToInt()}%" else "N/A"
        val oppDestroyerComebackPct = if (oppDestroyerSunkFirstGames > 0) "${(oppDestroyerSunkFirstLosses.toFloat() * 100 / oppDestroyerSunkFirstGames).roundToInt()}%" else "N/A"
        val firstShotPct = if (firstShotAttempts > 0) "${(firstShotHits.toFloat() * 100 / firstShotAttempts).roundToInt()}%" else "N/A"
        val oppFirstShotPct = if (oppFirstShotAttempts > 0) "${(oppFirstShotHits.toFloat() * 100 / oppFirstShotAttempts).roundToInt()}%" else "N/A"

        val basicStats = mapOf(
            "Matches Played" to targetGames.size.toString(),
            "Avg Match Length" to "$avgTotalShots Shots\n($avgTotalTurns Turns)",
            "Wins" to wins.size.toString(),
            "Losses" to losses.size.toString(),
            "Win Rate" to "$winRate%",
            "Loss Rate" to "$lossRate%",
            "Avg Win" to "$avgShotsToWin Shots\n($avgTurnsToWin Turns)",
            "Avg Loss" to "$avgShotsToLose Shots\n($avgTurnsToLose Turns)",
            "Fastest Victory" to if (fastestWinShots > 0) "$fastestWinShots Shots\n($fastestWinTurns Turns)" else "N/A",
            "Fastest Defeat" to if (fastestLossShots > 0) "$fastestLossShots Shots\n($fastestLossTurns Turns)" else "N/A",
            "Strike Accuracy" to "$accuracy%",
            "Opp. Accuracy" to "$oppAccuracy%"
        )

        val wildStats = mapOf(
            "First Strike Hit %" to firstShotPct,
            "Opp. First Str. Hit %" to oppFirstShotPct,
            "First Blood Win %" to firstBloodPct,
            "First Blood Loss %" to oppFirstBloodPct,
            "My Steal Back %" to myStealBackPct,
            "Opp. Steal Back %" to oppStealBackPct,
            "Destroyer Comeback" to destroyerComebackPct,
            "Opp. Dest. Comeback" to oppDestroyerComebackPct,
            "Flawless Victories" to flawlessWins.toString(),
            "Flawless Defeats" to flawlessLosses.toString(),
            "Clutch Victories" to clutchWins.toString(),
            "Clutch Defeats" to clutchLosses.toString(),
            "Ultra Clutch Wins" to ultraClutchWins.toString(),
            "Ultra Clutch Defeats" to ultraClutchLosses.toString()
        )

        Pair(basicStats, wildStats)
    }

    suspend fun getTrendData(
        dao: GameRepository,
        playerName: String? = null,
        opponentName: String? = null
    ): TrendData = withContext(Dispatchers.Default) {
        val allGames = dao.getAllGamesSync()
        val targetGames = allGames.filter { game ->
            val matchesPlayer = playerName == null || playerName == "All" || game.playerName.equals(playerName, ignoreCase = true)
            val matchesOpp = opponentName == null || opponentName == "All" || game.opponentName.equals(opponentName, ignoreCase = true)
            matchesPlayer && matchesOpp
        }.sortedBy { it.timestamp }

        val targetGameIds = targetGames.map { it.id }.toSet()
        val allMoves = if (targetGameIds.isNotEmpty()) dao.getMovesForGames(targetGameIds.toList()) else emptyList()

        val completedGames = targetGames.filter { it.result != null }
        val recentResults = completedGames.takeLast(15).map { it.result!! }

        val accuracyHistory = completedGames.mapNotNull { game ->
            val gameMoves = allMoves.filter { it.gameId == game.id && it.isOffense && it.result in listOf("HIT", "MISS") }
            if (gameMoves.isNotEmpty()) {
                val hits = gameMoves.count { it.result == "HIT" }
                (hits.toFloat() / gameMoves.size.toFloat()) * 100f
            } else null
        }

        // NEW: Calculate Opponent Accuracy Progression
        val oppAccuracyHistory = completedGames.mapNotNull { game ->
            val oppMoves = allMoves.filter { it.gameId == game.id && !it.isOffense && it.result in listOf("HIT", "MISS") }
            if (oppMoves.isNotEmpty()) {
                val hits = oppMoves.count { it.result == "HIT" }
                (hits.toFloat() / oppMoves.size.toFloat()) * 100f
            } else null
        }

        val sortedWins = completedGames.filter { it.result == "WIN" }.sortedBy { it.id }

        val timeToSunk = sortedWins.mapNotNull { game ->
            val playerShotsCount = allMoves.count { it.gameId == game.id && it.isOffense && !isShipData(it.result) }
            if (playerShotsCount > 0) playerShotsCount.toFloat() else null
        }

        // NEW: Calculate Damage Taken (Opponent Hits) during your victories
        val damageTakenHistory = sortedWins.mapNotNull { game ->
            val oppHits = allMoves.count { it.gameId == game.id && !it.isOffense && it.result == "HIT" }
            oppHits.toFloat()
        }

        // NEW: Rolling 5-Game Win Rate
        // Calculates the win percentage of the current game plus the 4 preceding games
        val rollingWinRateHistory = completedGames.mapIndexed { index, _ ->
            val windowStart = maxOf(0, index - 4)
            val window = completedGames.subList(windowStart, index + 1)
            val wins = window.count { it.result == "WIN" }
            (wins.toFloat() / window.size.toFloat()) * 100f
        }

        // NEW: Target Acquisition Speed
        // Counts how many shots were fired before the very first "HIT" was scored
        val targetAcquisitionHistory = completedGames.mapNotNull { game ->
            val offensiveShots = allMoves
                .filter { it.gameId == game.id && it.isOffense && !isShipData(it.result) }
                .sortedBy { it.shotNumber }

            val firstHitIndex = offensiveShots.indexOfFirst { it.result == "HIT" }
            if (firstHitIndex != -1) (firstHitIndex + 1).toFloat() else null
        }

        // Return the fully populated object
        TrendData(
            recentResults, accuracyHistory, timeToSunk, oppAccuracyHistory,
            damageTakenHistory, rollingWinRateHistory, targetAcquisitionHistory
        )
    }

    suspend fun fetchHeatmapData(
        dao: GameRepository,
        playerName: String? = null,
        opponentName: String? = null
    ): HeatmapData = withContext(Dispatchers.Default) {
        val allGames = dao.getAllGamesSync()
        val targetGames = allGames.filter { game ->
            val matchesPlayer = playerName == null || playerName == "All" || game.playerName.equals(playerName, ignoreCase = true)
            val matchesOpp = opponentName == null || opponentName == "All" || game.opponentName.equals(opponentName, ignoreCase = true)
            matchesPlayer && matchesOpp
        }
        val targetGameIds = targetGames.map { it.id }.toSet()
        val allMoves = if (targetGameIds.isNotEmpty()) dao.getMovesForGames(targetGameIds.toList()) else emptyList()

        val uS = mutableMapOf<Pair<Int, Int>, Int>()
        val oS = mutableMapOf<Pair<Int, Int>, Int>()
        val uA = mutableMapOf<Pair<Int, Int>, Int>()
        val oA = mutableMapOf<Pair<Int, Int>, Int>()
        val uH = mutableMapOf<Pair<Int, Int>, Int>()
        val oH = mutableMapOf<Pair<Int, Int>, Int>()

        allMoves.forEach { move ->
            val cell = Pair(move.x, move.y)
            if (isShipData(move.result)) {
                if (!move.isOffense) uS[cell] = (uS[cell] ?: 0) + 1
                else oS[cell] = (oS[cell] ?: 0) + 1
            } else {
                if (move.isOffense) {
                    uA[cell] = (uA[cell] ?: 0) + 1
                    if (move.result == "HIT") uH[cell] = (uH[cell] ?: 0) + 1 // Log the hit!
                } else {
                    oA[cell] = (oA[cell] ?: 0) + 1
                    if (move.result == "HIT") oH[cell] = (oH[cell] ?: 0) + 1 // Log the hit!
                }
            }
        }

        // 1. Let the Engine calculate the percentages on the background thread
        val uEfficiency = uA.mapValues { (cell, total) ->
            val hits = uH[cell] ?: 0
            if (total > 0) ((hits.toFloat() / total) * 100f).roundToInt() else 0
        }
        val oEfficiency = oA.mapValues { (cell, total) ->
            val hits = oH[cell] ?: 0
            if (total > 0) ((hits.toFloat() / total) * 100f).roundToInt() else 0
        }

        HeatmapData(
            userShips = uS,
            oppShips = oS,
            userAttacks = uA,
            oppAttacks = oA,
            userEfficiency = uEfficiency,
            oppEfficiency = oEfficiency,
            maxUserShips = uS.values.maxOrNull() ?: 1,
            maxOppShips = oS.values.maxOrNull() ?: 1,
            maxUserAttacks = uA.values.maxOrNull() ?: 1,
            maxOppAttacks = oA.values.maxOrNull() ?: 1,
            maxUserEfficiency = uEfficiency.values.maxOrNull() ?: 1,
            maxOppEfficiency = oEfficiency.values.maxOrNull() ?: 1
        )
    }
}