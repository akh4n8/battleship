package com.ak.battleship.analytics

import com.ak.battleship.data.Move

/**
 * ANALYTICS LAYER: Machine Learning Inference Engine
 * * Utilizes pairwise Base Win Rates extracted from R Studio.
 * * Automatically inverts log-odds based on who is actively playing.
 */
object InferenceEngine {

    // Format: Pair(PlayerWithAdvantage, Opponent) to Z-Score
    private val pairwiseBiases = mapOf(
        Pair("luki", "deepbluebot") to 12.21f,
        Pair("luki", "grandma") to 1.08f,
        Pair("richard", "luki") to -1.73f,
        Pair("luki", "nemesisbot") to 12.17f,
        Pair("luki", "watsonbot") to 12.77f,
        Pair("luki", "richard") to 12.15f

        // Example of adding a new R calculation later:
        // Pair("luki", "grandma") to 2.45f
    )

    // --- STRICT GAME THEORY CONSTANTS ---
    private const val WEIGHT_SUNK_DIFF = 1.2f
    private const val WEIGHT_MY_ACTIVE_BLOOD = 0.35f
    private const val WEIGHT_OPP_ACTIVE_BLOOD = -0.35f
    private const val WEIGHT_PLAYER_FIRST_BLOOD = 0.5f

    /**
     * Smart Router: Normalizes names to lowercase to prevent Case-Sensitivity mismatches.
     * Checks the map for the specific matchup.
     */
    private fun getPairwiseBias(player: String, opponent: String): Float {
        // Normalize everything to lowercase for the lookup
        val p = player.lowercase()
        val o = opponent.lowercase()

        // 1. Standard Match: Check if the normalized pair exists
        val directMatch = pairwiseBiases[Pair(p, o)]
        if (directMatch != null) return directMatch

        // 2. Inverted Match: Check if the reverse normalized pair exists
        val invertedMatch = pairwiseBiases[Pair(o, p)]
        if (invertedMatch != null) return -invertedMatch

        // 3. Unknown Match
        return 0.0f
    }

    fun calculateLiveWinState(
        gameMoves: List<Move>,
        playerName: String,
        opponentName: String
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
        val w0 = getPairwiseBias(playerName, opponentName)

        var z = w0 +
                (WEIGHT_SUNK_DIFF * sunkDiff) +
                (WEIGHT_MY_ACTIVE_BLOOD * myActiveBlood) +
                (WEIGHT_OPP_ACTIVE_BLOOD * oppActiveBlood)

        if (iGotFirstBlood) {
            z += WEIGHT_PLAYER_FIRST_BLOOD
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