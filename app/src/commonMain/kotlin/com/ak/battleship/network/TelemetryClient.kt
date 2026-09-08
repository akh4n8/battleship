package com.ak.battleship.network

expect class TelemetryClient() {
    fun sendMatchData(url: String, anonKey: String, payload: String)
}

fun buildTelemetryPayload(
    playerAlias: String,
    opponent: String,
    gameMode: String,
    outcome: String,
    turnCount: Int,
    movesJson: String
): String {
    // Escaping JSON properly for REST API payload
    val safeAlias = playerAlias.replace("\"", "\\\"")
    val safeOpponent = opponent.replace("\"", "\\\"")
    val safeMode = gameMode.replace("\"", "\\\"")
    val safeOutcome = outcome.replace("\"", "\\\"")
    
    return """
        {
            "player_alias": "$safeAlias",
            "opponent": "$safeOpponent",
            "game_mode": "$safeMode",
            "outcome": "$safeOutcome",
            "turn_count": $turnCount,
            "moves": $movesJson
        }
    """.trimIndent()
}
