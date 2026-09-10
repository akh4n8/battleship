package com.ak.battleship.network

import com.ak.battleship.data.Game
import com.ak.battleship.data.Move
import com.ak.battleship.model.isShipData
import com.ak.battleship.utils.reconstructFleetFromMoves
import kotlin.random.Random

expect class TelemetryClient() {
    fun sendMatchData(url: String, anonKey: String, payload: String)
}

/**
 * Generates an RFC 4122 compliant UUID v4 to ensure game IDs are globally unique across all players.
 */
fun generateUniqueMatchId(): String {
    return com.ak.battleship.PlatformServices.generateUUID()
}

fun buildTelemetryBatchPayload(
    game: Game,
    allMoves: List<Move>,
    formattedTimestamp: String
): String {
    val uniqueMatchId = generateUniqueMatchId()

    val gameMoves = allMoves.filter { it.gameId == game.id }.sortedWith(compareBy({ it.turnNumber }, { it.shotNumber }))
    val offFleet = reconstructFleetFromMoves(gameMoves.filter { it.isOffense })
    val defFleet = reconstructFleetFromMoves(gameMoves.filter { !it.isOffense })

    val rows = gameMoves.map { move ->
        val grid = if (move.isOffense) "Offense" else "Defense"
        val coordinate = "${(move.y + 65).toChar()}${move.x + 1}"
        var finalResultStr = move.result

        if (isShipData(finalResultStr)) {
            val fleetToCheck = if (move.isOffense) offFleet else defFleet
            finalResultStr = fleetToCheck.find { it.getCells().contains(Pair(move.x, move.y)) }?.name ?: "SHIP"
        } else if (move.isSunk) {
            finalResultStr = "SUNK"
        }

        val safeOutcome = (game.result ?: "").replace("\"", "\\\"")
        val safeMode = game.gameMode.replace("\"", "\\\"")
        
        // Ensure local multiplayer modes anonymize the opponent's typed name
        val rawOpponent = if (game.gameMode == "PassAndPlay" || game.gameMode == "Companion") "Anonymous" else game.opponentName
        val safeOpponent = rawOpponent.replace("\"", "\\\"")
        
        val safeTime = formattedTimestamp.replace("\"", "\\\"")
        val safeResult = finalResultStr.replace("\"", "\\\"")
        val uniqueRowId = com.ak.battleship.PlatformServices.generateUUID() // Safely prevent Supabase overwrites if they have 'id' as PK

        """{"game_id":"$uniqueMatchId","match_outcome":"$safeOutcome","game_mode":"$safeMode","player":"Anonymous","opponent":"$safeOpponent","timestamp":"$safeTime","turn_number":${move.turnNumber},"shot_number":${move.shotNumber},"grid":"$grid","x":${move.x},"y":${move.y},"coordinate":"$coordinate","shot_result":"$safeResult"}"""
    }

    return "[" + rows.joinToString(",") + "]"
}
