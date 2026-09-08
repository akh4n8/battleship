with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'r') as f:
    text = f.read()

bad = """    // --- TELEMETRY ---
    var telemetryOptIn by mutableStateOf(settingsManager.getBoolean("telemetry_opt_in", false))
        private set
    var playerAlias by mutableStateOf(settingsManager.getString("player_alias", "Anonymous"))
        private set"""

good = """    // --- TELEMETRY ---
    var telemetryOptIn by mutableStateOf(false)
        private set
    var playerAlias by mutableStateOf("Anonymous")
        private set"""

text = text.replace(bad, good)

# Find init block and inject
init_target = "observeCurrentGameMoves(it)"
init_inject = """observeCurrentGameMoves(it)
        }
        
        telemetryOptIn = settingsManager.getBoolean("telemetry_opt_in", false)
        playerAlias = settingsManager.getString("player_alias", "Anonymous")"""

text = text.replace("observeCurrentGameMoves(it)\n        }", init_inject)

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'w') as f:
    f.write(text)
