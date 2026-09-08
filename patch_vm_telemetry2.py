import re

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'r') as f:
    text = f.read()

# Add imports
imports = """import com.ak.battleship.utils.SettingsManager
import com.ak.battleship.network.TelemetryClient
import com.ak.battleship.network.buildTelemetryPayload"""
text = text.replace('import com.ak.battleship.utils.SettingsManager', imports)

# Add properties
props = """
    // --- TELEMETRY ---
    var telemetryOptIn by mutableStateOf(settingsManager.getBoolean("telemetry_opt_in", false))
        private set
    var playerAlias by mutableStateOf(settingsManager.getString("player_alias", "Anonymous"))
        private set

    fun setTelemetryOptIn(optIn: Boolean) {
        telemetryOptIn = optIn
        settingsManager.setBoolean("telemetry_opt_in", optIn)
    }

    fun setPlayerAlias(alias: String) {
        playerAlias = alias
        settingsManager.setString("player_alias", alias)
    }

    // --- GAME STATE ---"""
text = text.replace('    // --- GAME STATE ---', props)

# Insert logic inside saveOpponentShipsAndFinish
target_line = "currentPhase = GamePhase.BATTLE"
logic = """currentPhase = GamePhase.BATTLE
                
                // TELEMETRY POST
                if (telemetryOptIn) {
                    val allMoves = _currentMoves.value
                    val movesJson = "[" + allMoves.joinToString(",") { m ->
                        "\"{\\\"turn\\\":${m.turnNumber},\\\"x\\\":${m.x},\\\"y\\\":${m.y},\\\"result\\\":\\\"${m.result}\\\",\\\"is_offense\\\":${m.isOffense}}\""
                    } + "]"
                    val payload = buildTelemetryPayload(
                        playerAlias = playerAlias,
                        opponent = activeGame.opponentName,
                        gameMode = activeGame.gameMode,
                        outcome = finalResult,
                        turnCount = allMoves.size,
                        movesJson = movesJson
                    )
                    TelemetryClient().sendMatchData(
                        url = "https://trizdhkyiuahznicbtij.supabase.co",
                        anonKey = "sb_publishable_KKIBHotkK0eCsBCHYt1PWA_XZ-nw2j_",
                        payload = payload
                    )
                }"""
text = text.replace(target_line, logic)

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'w') as f:
    f.write(text)
