package com.ak.battleship.network

import com.ak.battleship.data.Game
import com.ak.battleship.data.Move
import com.ak.battleship.model.isShipData
import com.ak.battleship.utils.reconstructFleetFromMoves

expect class TelemetryClient() {
    fun sendMatchData(url: String, anonKey: String, payload: String)
}

fun buildTelemetryBatchPayload(
    game: Game,
    allMoves: List<Move>,
    formattedTimestamp: String
): String {
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

        val safeGameId = game.id.toString().replace("\"", "\\\"")
        val safeOutcome = (game.result ?: "").replace("\"", "\\\"")
        val safeMode = game.gameMode.replace("\"", "\\\"")
        val safeOpponent = game.opponentName.replace("\"", "\\\"")
        val safeTime = formattedTimestamp.replace("\"", "\\\"")
        val safeResult = finalResultStr.replace("\"", "\\\"")

        """{"game_id":"$safeGameId","match_outcome":"$safeOutcome","game_mode":"$safeMode","player":"Anonymous","opponent":"$safeOpponent","timestamp":"$safeTime","turn_number":${move.turnNumber},"shot_number":${move.shotNumber},"grid":"$grid","x":${move.x},"y":${move.y},"coordinate":"$coordinate","shot_result":"$safeResult"}"""
    }

    return "[" + rows.joinToString(",") + "]"
}
