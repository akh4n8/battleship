import re

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'r') as f:
    content = f.read()

# Add imports
if 'import com.ak.battleship.network.TelemetryClient' not in content:
    content = content.replace('import com.ak.battleship.utils.SettingsManager', 
                              'import com.ak.battleship.utils.SettingsManager\nimport com.ak.battleship.network.TelemetryClient\nimport com.ak.battleship.network.buildTelemetryPayload')

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
"""
if 'var telemetryOptIn' not in content:
    content = content.replace('// --- GAME STATE ---', props + '\n    // --- GAME STATE ---')

# Add the firing logic at the end of saveOpponentShipsAndFinish
fire_logic = """
            currentGame?.let { activeGame ->
                val updatedGame = activeGame.copy(result = finalResult)
                dao.updateGame(updatedGame)
                currentGame = updatedGame
                widgetShotTriggerKey = 0
                currentPhase = GamePhase.BATTLE
                
                // TELEMETRY POST
                if (telemetryOptIn) {
                    val allMoves = _currentMoves.value
                    val movesJson = "[" + allMoves.joinToString(",") { m ->
                        "{\"turn\":${m.turnNumber},\"x\":${m.x},\"y\":${m.y},\"result\":\"${m.result}\",\"is_offense\":${m.isOffense}}"
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
                }
            }
"""

content = re.sub(r'currentGame\?\.let \{ activeGame ->.*?currentPhase = GamePhase\.BATTLE\s*\}', fire_logic.strip(), content, flags=re.DOTALL)

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'w') as f:
    f.write(content)

