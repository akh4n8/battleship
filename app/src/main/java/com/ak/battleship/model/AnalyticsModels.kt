package com.ak.battleship.model

/**
 * CORE DOMAIN: Data Transfer Objects (DTOs)
 * * These immutable classes transport complex analytical and tactical data
 * between the Engines (AI/Stats) and the Presentation Layer (ViewModel/UI).
 */

/**
 * Holds the raw coordinate generation data for the post-game heatmaps.
 */
data class HeatmapData(
    val userShips: Map<Pair<Int, Int>, Int>,
    val oppShips: Map<Pair<Int, Int>, Int>,
    val userAttacks: Map<Pair<Int, Int>, Int>,
    val oppAttacks: Map<Pair<Int, Int>, Int>,
    val userEfficiency: Map<Pair<Int, Int>, Int>, // NEW: Pre-calculated in the Engine
    val oppEfficiency: Map<Pair<Int, Int>, Int>,  // NEW: Pre-calculated in the Engine
    val maxUserShips: Int,
    val maxOppShips: Int,
    val maxUserAttacks: Int,
    val maxOppAttacks: Int,
    val maxUserEfficiency: Int,
    val maxOppEfficiency: Int
)
/**
 * Transports chronological statistical metrics for the Player vs. Opponent line charts.
 */
data class TrendData(
    val winLossHistory: List<String>,
    val accuracyHistory: List<Float>,
    val timeToSunkHistory: List<Float>,
    val oppAccuracyHistory: List<Float>,
    val damageTakenHistory: List<Float>,
    // --- NEW METRICS ---
    val rollingWinRateHistory: List<Float>,
    val targetAcquisitionHistory: List<Float>
)

/**
 * Represents a single tactical choice made by the AI.
 * * @property coordinate The exact (x, y) target the bot chose to fire at.
 * @property log A human-readable reasoning string for the UI (e.g., "Standard Density Map").
 * @property heatMap The 10x10 array of mathematical weights used to make the decision.
 */
data class BotDecision(
    val coordinate: Pair<Int, Int>,
    val log: String,
    val heatMap: Array<IntArray>,
    val diagnosticMap: Array<IntArray>? = null // NEW: Holds the AI's internal peg states
)
enum class ShotOutcome { MISS, HIT, SUNK }

enum class PlaybackCommand {
    IDLE,
    TIME_TRAVEL_BACKWARD,    // User hit previous or scrubbed left
    TIME_TRAVEL_FORWARD,     // User skipped multiple turns forward
    PLAY_MISS,               // Live turn: Miss
    PLAY_HIT,                // Live turn: Hit
    PLAY_SUNK,               // Live turn: Sunk (but game continues)
    PLAY_WIN_LIVE,           // Game-winning shot (Live game or Autoplay)
    PLAY_WIN_CINEMATIC,      // Opening a finished game (Win)
    PLAY_LOSS_CINEMATIC,     // Opening a finished game (Loss)
    PLAY_JUMP                // Rapid scrubbing or snapping to timeline
}