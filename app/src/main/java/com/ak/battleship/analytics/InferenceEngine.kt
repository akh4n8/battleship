package com.ak.battleship.analytics

import com.ak.battleship.data.Move
import com.ak.battleship.data.Game

/**
 * ANALYTICS LAYER: Dynamic Machine Learning Inference Engine
 * * Calculates pairwise Base Win Rates dynamically from Room SQLite.
 * * Uses Bayesian Add-5 Smoothing to prevent overfitting on new matchups.
 */
object InferenceEngine {

    // --- STRICT GAME THEORY CONSTANTS ---
    private const val WEIGHT_SUNK_DIFF = 1.2f
    private const val WEIGHT_MY_ACTIVE_BLOOD = 0.35f
    private const val WEIGHT_OPP_ACTIVE_BLOOD = -0.35f
    private const val WEIGHT_PLAYER_FIRST_BLOOD = 0.5f

    /**
     * Dynamically calculates Log-Odds (Z-Score) based on historical match data.
     */
    private fun getPairwiseBias(player: String, opponent: String, allGames: List<Game>): Float {
        val p = player.lowercase()
        val o = opponent.lowercase()

        val matchHistory = allGames.filter { game ->
            game.result != null &&
            ((game.playerName.equals(p, ignoreCase = true) && game.opponentName.equals(o, ignoreCase = true)) ||
             (game.playerName.equals(o, ignoreCase = true) && game.opponentName.equals(p, ignoreCase = true)))
        }

        var playerWins = 0
        var playerLosses = 0

        for (game in matchHistory) {
            val isP1 = game.playerName.equals(p, ignoreCase = true)
            if (isP1) {
                if (game.result == "WIN") playerWins++ else if (game.result == "LOSS") playerLosses++
            } else {
                if (game.result == "LOSS") playerWins++ else if (game.result == "WIN") playerLosses++
            }
        }

        // Bayesian Add-5 Smoothing
        val pWin = (playerWins + 5).toFloat() / (playerWins + playerLosses + 10).toFloat()
        
        // Log-odds
        return kotlin.math.ln((pWin / (1.0f - pWin)).toDouble()).toFloat()
    }

    fun calculateLiveWinState(
        gameMoves: List<Move>,
        playerName: String,
        opponentName: String,
        allGames: List<Game>
    ): Pair<Float, List<Pair<String, String>>> {

        var myHits = 0
        var mySunks = 0
        var oppHits = 0
        var oppSunks = 0
        var iGotFirstBlood = false

        val firstHitMove = gameMoves.find { it.result == "HIT" || it.isSunk }
        if (firstHitMove != null && firstHitMove.isOffense) {
            iGotFirstBlood = true
        }

        gameMoves.forEach { move ->
            if (move.isOffense) {
                if (move.result == "HIT" || move.isSunk) myHits++
                if (move.isSunk) mySunks++
            } else {
                if (move.result == "HIT" || move.isSunk) oppHits++
                if (move.isSunk) oppSunks++
            }
        }

        val sunkDiff = mySunks - oppSunks
        val myActiveBlood = Math.max(0.0, myHits - (mySunks * 3.4)).toFloat()
        val oppActiveBlood = Math.max(0.0, oppHits - (oppSunks * 3.4)).toFloat()

        // --- FETCH THE DYNAMIC PAIRWISE BIAS ---
        val w0 = getPairwiseBias(playerName, opponentName, allGames)

        var z = w0 +
                (WEIGHT_SUNK_DIFF * sunkDiff) +
                (WEIGHT_MY_ACTIVE_BLOOD * myActiveBlood) +
                (WEIGHT_OPP_ACTIVE_BLOOD * oppActiveBlood)

        // Decay First Blood over time (assumes a max game length of ~40 turns)
        val turnCount = gameMoves.count { it.result == "MISS" || it.result == "HIT" || it.isSunk } / 2f
        val firstBloodDecay = Math.max(0.0, 1.0 - (turnCount / 40.0)).toFloat()

        if (iGotFirstBlood) {
            z += (WEIGHT_PLAYER_FIRST_BLOOD * firstBloodDecay)
        }

        val clampedZ = z.coerceIn(-20f, 20f)
        val rawProbability = (1.0 / (1.0 + kotlin.math.exp(-clampedZ.toDouble()))).toFloat()

        // THE FIX: Clamp to exactly 1% and 99%
        val probability = rawProbability.coerceIn(0.01f, 0.99f)

        val hudStats = listOf(
            Pair("SNK-DFF", if (sunkDiff > 0) "+$sunkDiff" else "$sunkDiff"),
            Pair("TRG-LCK", "${myActiveBlood.toInt()} EST"),
            Pair("1ST-BLD", if (iGotFirstBlood) "ACQUIRED" else "LOST")
        )

        return Pair(probability, hudStats)
    }
}