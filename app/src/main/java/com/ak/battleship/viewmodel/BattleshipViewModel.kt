package com.ak.battleship.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ak.battleship.ai.TacticalEngine
import com.ak.battleship.analytics.InferenceEngine
import com.ak.battleship.analytics.TrendEngine
import com.ak.battleship.data.BattleshipDao
import com.ak.battleship.data.Game
import com.ak.battleship.data.Move
import com.ak.battleship.data.OpponentProfile
import com.ak.battleship.model.GamePhase
import com.ak.battleship.model.Ship
import com.ak.battleship.model.isShipData
import com.ak.battleship.utils.guessFleetFromAnonymousCoords
import com.ak.battleship.utils.reconstructFleetFromMoves
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.firstOrNull

enum class WidgetTheme(val title: String) {
    STANDARD_GAUGE("Tactical Gauge"),
    BROADSIDE("The High Seas"),
    SYMPHONY_CONDUCTOR("Orchestral Score"),
    ICE_FISHING("Aqua-Vu Camera"),
    WALL_STREET("Market Terminal"),
    POKER("High Stakes"),
    ILLUSIONIST("The Magician"),
    RETRO_PET("Meter Pet"),
    WEATHER("Local Forecast"),
    RPG_GUILD("Guild Master"),
    ELDRICTH_SEANCE("The Abyss"),
    LET_HIM_COOK("Kitchen Cam"),
    BOMB_DEFUSAL("EOD Unit")
}

private data class HumanMatchContext(
    val humanShipPegs: List<Move>,
    val humanHuntShots: List<Move>,
    val humanWon: Boolean
)

class BattleshipViewModel(private val dao: BattleshipDao, private val context: Context) : ViewModel() {
    // --- DEBUGGING LAYERS ---
    var isHeatmapVisible by mutableStateOf(false); private set
    var isHeatmapNumbersVisible by mutableStateOf(false); private set
    var isDiagnosticVisible by mutableStateOf(false); private set

    fun toggleHeatmap() { isHeatmapVisible = !isHeatmapVisible }
    fun toggleHeatmapNumbers() { isHeatmapNumbersVisible = !isHeatmapNumbersVisible }
    fun toggleDiagnostic() { isDiagnosticVisible = !isDiagnosticVisible }

    private val _gameModeFilter = MutableStateFlow("All")
    val gameModeFilter: StateFlow<String> = _gameModeFilter.asStateFlow()
    fun setGameModeFilter(filter: String) { _gameModeFilter.value = filter }

    private val _opponentFilter = MutableStateFlow("All")
    val opponentFilter: StateFlow<String> = _opponentFilter.asStateFlow()

    private val _playerFilter = MutableStateFlow("All")
    val playerFilter: StateFlow<String> = _playerFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow("All")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()
    fun setStatusFilter(filter: String) { _statusFilter.value = filter }

    val allGames = combine(
        dao.getAllGames(),
        _gameModeFilter,
        _opponentFilter,
        _playerFilter,
        _statusFilter
    ) { games, modeFilter, oppFilter, playFilter, statFilter -> // <-- EXACT MATCH NOW
        games.filter { game ->
            val matchesOpponent = oppFilter == "All" || game.opponentName.equals(oppFilter, ignoreCase = true)
            val matchesPlayer = playFilter == "All" || game.playerName.equals(playFilter, ignoreCase = true)

            val matchesStatus = when (statFilter) {
                "Completed" -> game.result != null
                "In Progress" -> game.result == null
                else -> true
            }

            val matchesMode = modeFilter == "All" || game.gameMode.equals(modeFilter, ignoreCase = true)

            matchesOpponent && matchesPlayer && matchesStatus && matchesMode
        }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Derive human players dynamically from all games
    val playerSuggestions = dao.getAllGames().map { games ->
        games.map { it.playerName }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Derive ONLY human opponents (filter out the Bots)
    val opponentSuggestions = dao.getAllGames().map { games ->
        games.filter { it.gameMode == "Companion" || it.gameMode == "PassAndPlay" }
            .map { it.opponentName }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyOpponentSuggestions = combine(
        dao.getAllGames(),
        _playerFilter,
        _gameModeFilter // NEW: Inject the mode filter
    ) { games, playFilter, modeFilter ->
        games.filter { game ->
            val matchesPlayer = playFilter == "All" || game.playerName.equals(playFilter, ignoreCase = true)
            val matchesMode = modeFilter == "All" || game.gameMode.equals(modeFilter, ignoreCase = true)
            matchesPlayer && matchesMode
        }.map { it.opponentName }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyPlayerSuggestions = combine(
        dao.getAllGames(),
        _opponentFilter,
        _gameModeFilter // NEW: Inject the mode filter
    ) { games, oppFilter, modeFilter ->
        games.filter { game ->
            val matchesOpponent = oppFilter == "All" || game.opponentName.equals(oppFilter, ignoreCase = true)
            val matchesMode = modeFilter == "All" || game.gameMode.equals(modeFilter, ignoreCase = true)
            matchesOpponent && matchesMode
        }.map { it.playerName }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- UI SCROLL MEMORY ---
    var historyScrollIndex by mutableIntStateOf(0)
    var historyScrollOffset by mutableIntStateOf(0)

    var currentGameId by mutableStateOf<Int?>(null); private set
    var currentGame by mutableStateOf<Game?>(null); private set
    var isOffenseMode by mutableStateOf(true); private set
    fun setOffenseTab(isOffense: Boolean) { isOffenseMode = isOffense }

    var currentPhase by mutableStateOf(GamePhase.BATTLE); private set
    var pendingResult by mutableStateOf<String?>(null); private set

    var isAnalyticsScreenVisible by mutableStateOf(false); private set

    fun openAnalytics() {
        isAnalyticsScreenVisible = true

        // THE FIX: Reset hidden filters so the Analytics dropdowns can access the whole database!
        setGameModeFilter("All")
        setStatusFilter("All")
    }

    fun closeAnalytics() { isAnalyticsScreenVisible = false }

    val placementFleet = mutableStateListOf<Ship>()
    private val _currentMoves = MutableStateFlow<List<Move>>(emptyList())
    val currentMoves: StateFlow<List<Move>> = _currentMoves.asStateFlow()

    // --- TIME MACHINE REPLAY STATE ---
    var isPlaybackMode by mutableStateOf(false); private set
    var playbackIndex by mutableIntStateOf(-1); private set
    var playbackMaxIndex by mutableIntStateOf(0); private set

    // NEW: Tracks if the user is currently holding the slider
    var isScrubbing by mutableStateOf(false); private set
    fun setIsScrubbing(scrubbing: Boolean) { isScrubbing = scrubbing }

    // Allows the UI to jump to a specific moment in time
    fun setPlaybackPosition(index: Int) {
        // THE FIX: Allow -1 through the security gate
        if (isPlaybackMode && index in -1..playbackMaxIndex) {
            playbackIndex = index
        }
    }

    // The UI will observe THIS flow instead of currentMoves
    val displayMoves: StateFlow<List<Move>> = combine(
        _currentMoves,
        snapshotFlow { playbackIndex }
    ) { moves, index ->
        // 1. Always keep the ships on the board
        val shipMoves = moves.filter { isShipData(it.result) }

        // 2. Sort the combat moves chronologically
        val combatMoves = moves.filter { !isShipData(it.result) }
            .sortedWith(compareBy({ it.turnNumber }, { it.shotNumber }))

        // 3. Update the max index so the UI slider knows how far it can go
        playbackMaxIndex = combatMoves.size

        // 4. Slice the timeline!
        val activeCombatMoves = if (index == -1 || index >= combatMoves.size) {
            combatMoves // Show everything
        } else {
            combatMoves.take(index) // Slice off the future
        }

        // Recombine and send to the UI
        shipMoves + activeCombatMoves
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var movesJob: Job? = null
    private var botTurnJob: Job? = null

    // --- ANALYTICS ENGINE INTEGRATION ---
    private val _liveHudStats = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val liveHudStats: StateFlow<List<Pair<String, String>>> = _liveHudStats.asStateFlow()

    val liveWinProbability: StateFlow<Float> = displayMoves.map { moves ->
        val oppName = currentGame?.opponentName ?: "Unknown"
        val playerName = currentGame?.playerName ?: "Player 1" // NEW: Grab the player name

        // Pass BOTH names into the engine
        val (prob, stats) = InferenceEngine.calculateLiveWinState(moves, playerName, oppName)

        _liveHudStats.value = stats
        prob
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.5f)

    var isActionLocked by mutableStateOf(false); private set
    var botSecretFleet by mutableStateOf<List<Ship>>(emptyList())
    var currentPlayer by mutableStateOf("Human")

    var widgetShotOutcome by mutableStateOf("MISS"); private set
    var widgetShotTriggerKey by mutableStateOf(0); private set
    var widgetIsPlayerFiring by mutableStateOf(true); private set

    var isP1SetupPending by mutableStateOf(false); private set
    var isP2SetupPending by mutableStateOf(false); private set
    var activePlayer by mutableStateOf(1); private set

    // NEW: Loading state for heavy AI computations
    var isAiCalculating by mutableStateOf(false); private set

    // 1. Add these variables to the top of your ViewModel (near the other State variables):
    var adlerOffensiveMatrix by mutableStateOf<Array<FloatArray>?>(null); private set
    var adlerDefensiveMatrix by mutableStateOf<Array<FloatArray>?>(null); private set
    var moriartyPriorMatrix by mutableStateOf<Array<FloatArray>?>(null); private set // <-- NEW

    var lastBotDecision by mutableStateOf<String?>(null); private set
    var lastBotHeatmap by mutableStateOf<Array<IntArray>?>(null); private set
    var lastBotDiagnosticMap by mutableStateOf<Array<IntArray>?>(null); private set

    private val _currentWidgetTheme = MutableStateFlow(WidgetTheme.STANDARD_GAUGE)
    val currentWidgetTheme: StateFlow<WidgetTheme> = _currentWidgetTheme.asStateFlow()

    private fun clearTemporalState() {
        _currentMoves.value = emptyList()
        lastBotDecision = null
        lastBotHeatmap = null
        botSecretFleet = emptyList()
        isActionLocked = false
        pendingResult = null
        widgetShotTriggerKey = 0
        isPlaybackMode = false
        playbackIndex = -1
        initPlacementFleet()

        // RESET matrix states to avoid contamination
        adlerOffensiveMatrix = null
        adlerDefensiveMatrix = null

        // THE FIX: Ensure clean slate for debug layers on load
        isHeatmapVisible = false
        isHeatmapNumbersVisible = false
        isDiagnosticVisible = false
    }

    fun cycleWidgetTheme() {
        val themes = WidgetTheme.values()
        val nextIndex = (themes.indexOf(_currentWidgetTheme.value) + 1) % themes.size
        _currentWidgetTheme.value = themes[nextIndex]
    }

    fun setOpponentFilter(filter: String) { _opponentFilter.value = filter }
    fun setPlayerFilter(filter: String) { _playerFilter.value = filter }

    suspend fun getOpponentProfile(name: String): OpponentProfile? = dao.getProfile(name)
    fun saveOpponentProfile(name: String, notes: String, strategy: String) { viewModelScope.launch { dao.insertProfile(OpponentProfile(name, notes, strategy)) } }

    // --- DELEGATE TO TREND ENGINE ---
    suspend fun getStats(playerName: String? = null, opponentName: String? = null) = TrendEngine.getStats(dao, playerName, opponentName)
    suspend fun getTrendData(playerName: String? = null, opponentName: String? = null) = TrendEngine.getTrendData(dao, playerName, opponentName)
    suspend fun fetchHeatmapData(playerName: String? = null, opponentName: String? = null) = TrendEngine.fetchHeatmapData(dao, playerName, opponentName)

    suspend fun getTimeToSunkForOpponent(name: String): List<Pair<Long, Int>> {
        val games = dao.getAllGamesSync().filter { it.opponentName == name }
        val moves = dao.getAllMovesSync()
        return games.map { game ->
            val sunkTurn = moves.find { it.gameId == game.id && it.isSunk }?.turnNumber ?: 0
            game.timestamp to sunkTurn
        }.sortedBy { it.first }
    }

    private fun initPlacementFleet() {
        placementFleet.clear()
        placementFleet.addAll(listOf(
            Ship("Carrier", 5, 0f, 11f), Ship("Battleship", 4, 6f, 11f),
            Ship("Cruiser", 3, 0f, 13f), Ship("Submarine", 3, 4f, 13f), Ship("Destroyer", 2, 8f, 13f)
        ))
    }

    fun startGame(gameMode: String, playerName: String, opponentName: String) {
        currentPlayer = playerName
        activePlayer = 1
        isP1SetupPending = (gameMode == "PassAndPlay")
        isP2SetupPending = (gameMode == "PassAndPlay")

        viewModelScope.launch {
            clearTemporalState()

            // THE FIX: Calculate the brain right now using SQLite
            isAiCalculating = true
            val allGames = dao.getAllGamesSync()
            val allMoves = dao.getAllMovesSync()

            var adlerOff: Array<FloatArray>? = null
            var adlerDef: Array<FloatArray>? = null
            var mlPrior: Array<FloatArray>? = null

            if (opponentName.contains("Moriarty", ignoreCase = true) || playerName.contains("Moriarty", ignoreCase = true)) {
                val human = if (opponentName.contains("Moriarty", ignoreCase = true)) playerName else opponentName
                mlPrior = com.ak.battleship.ai.MoriartyBot.getPsychologicalPrior(context, human, allGames, allMoves)
            } else if (opponentName.contains("Adler", ignoreCase = true) || playerName.contains("Adler", ignoreCase = true)) {
                val human = if (opponentName.contains("Adler", ignoreCase = true)) playerName else opponentName
                val (o, d) = calculatePriorsForPlayer(human, allGames, allMoves)
                adlerOff = o
                adlerDef = d
            }

            adlerOffensiveMatrix = adlerOff
            adlerDefensiveMatrix = adlerDef
            moriartyPriorMatrix = mlPrior

            val metadataMap = mutableMapOf<String, String>()
            if (adlerOff != null) metadataMap["adler_offense"] = serializeMatrix(adlerOff)
            if (adlerDef != null) metadataMap["adler_defense"] = serializeMatrix(adlerDef)
            if (mlPrior != null) metadataMap["moriarty_prior"] = serializeMatrix(mlPrior)

            val metadataString = if (metadataMap.isNotEmpty()) serializeMetadata(metadataMap) else null
            isAiCalculating = false

            // THE FIX: Embed the snapshot into the Game row immediately!
            val gameId = dao.insertGame(Game(
                playerName = playerName,
                opponentName = opponentName,
                gameMode = gameMode,
                botBrainMetadata = metadataString
            ))

            currentGameId = gameId.toInt()
            currentGame = dao.getGameById(currentGameId!!)

            if (gameMode == "PassAndPlay") {
                currentPhase = GamePhase.HANDOFF
            } else {
                currentPhase = GamePhase.PLACEMENT
                isOffenseMode = false
            }

            if (gameMode == "Bot") {
                botSecretFleet = TacticalEngine.generateBotFleet(opponentName, adlerDefensiveMatrix)
                saveBotFleetToDatabaseInternal()
            }
            observeCurrentGameMoves(currentGameId!!)
        }
    }

    fun loadGame(gameId: Int, context: Context) {
        viewModelScope.launch {
            loadGameSuspended(gameId, context)
        }
    }

    // Extract the exact body of your old loadGame into this suspend function
    private suspend fun loadGameSuspended(gameId: Int, context: Context) {
        clearTemporalState()
        currentGameId = gameId
        currentGame = dao.getGameById(gameId)
        pendingResult = currentGame?.result
        activePlayer = 1

        val currentLocalGame = currentGame
        val isAdler = currentLocalGame != null && (currentLocalGame.opponentName.contains("Adler", ignoreCase = true) || currentLocalGame.playerName.contains("Adler", ignoreCase = true))
        val isMoriarty = currentLocalGame != null && (currentLocalGame.opponentName.contains("Moriarty", ignoreCase = true) || currentLocalGame.playerName.contains("Moriarty", ignoreCase = true))

        if (isAdler || isMoriarty) {
            val humanName = if (currentLocalGame!!.opponentName.contains("Adler", ignoreCase = true) || currentLocalGame.opponentName.contains("Moriarty", ignoreCase = true)) currentLocalGame.playerName else currentLocalGame.opponentName

            // 1. Check for Snapshots first!
            if (currentLocalGame.botBrainMetadata != null) {
                val metadata = parseMetadata(currentLocalGame.botBrainMetadata)
                metadata["adler_offense"]?.let { adlerOffensiveMatrix = deserializeMatrix(it) }
                metadata["adler_defense"]?.let { adlerDefensiveMatrix = deserializeMatrix(it) }
                metadata["moriarty_prior"]?.let { moriartyPriorMatrix = deserializeMatrix(it) } // <-- NEW
            } else {
                // 2. Fallback to dynamic reconstruction for legacy games
                isAiCalculating = true
                withContext(Dispatchers.Default) {
                    val allGames = dao.getAllGamesSync()
                    val allMoves = dao.getAllMovesSync()

                    val metadataMap = mutableMapOf<String, String>()

                    if (isMoriarty) {
                        val mlPrior = com.ak.battleship.ai.MoriartyBot.getPsychologicalPrior(context, humanName, allGames, allMoves, currentLocalGame.timestamp)
                        moriartyPriorMatrix = mlPrior
                        if (mlPrior != null) metadataMap["moriarty_prior"] = serializeMatrix(mlPrior)
                    } else {
                        val (pitOffense, pitDefense) = calculatePriorsForPlayer(humanName, allGames, allMoves, currentLocalGame.timestamp)
                        adlerOffensiveMatrix = pitOffense
                        adlerDefensiveMatrix = pitDefense
                        if (pitOffense != null) metadataMap["adler_offense"] = serializeMatrix(pitOffense)
                        if (pitDefense != null) metadataMap["adler_defense"] = serializeMatrix(pitDefense)
                    }

                    // SAVE back to DB to repair the legacy record
                    val repairedGame = currentLocalGame.copy(
                        botBrainMetadata = if (metadataMap.isNotEmpty()) serializeMetadata(metadataMap) else null
                    )
                    dao.updateGame(repairedGame)
                }
                isAiCalculating = false
            }
        }

        val initialMoves = dao.getMovesForGameSync(gameId)
        _currentMoves.value = initialMoves

        val isPassAndPlay = currentGame?.gameMode == "PassAndPlay"
        val isBotGame = currentGame?.gameMode == "Bot"

        val p1ShipMoves = initialMoves.filter { !it.isOffense && isShipData(it.result) }
        val p1Fleet = reconstructFleetFromMoves(p1ShipMoves)
        if (p1Fleet.isNotEmpty()) {
            placementFleet.clear()
            placementFleet.addAll(p1Fleet)
        }

        val p2ShipMoves = initialMoves.filter { it.isOffense && isShipData(it.result) }
        val p2Fleet = reconstructFleetFromMoves(p2ShipMoves)
        if (isBotGame && p2Fleet.isNotEmpty()) {
            botSecretFleet = p2Fleet
        }

        // ==========================================
        // THE FIX: EXPLICIT SETUP VERIFICATION
        // ==========================================
        val p1PlacedCount = p1ShipMoves.size
        val p2PlacedCount = p2ShipMoves.size

        val isSetupComplete = if (isPassAndPlay) {
            p1PlacedCount >= 17 && p2PlacedCount >= 17
        } else {
            p1PlacedCount >= 17
        }

        // Filter the combat moves early so we can use them
        val combatMoves = initialMoves.filter { !isShipData(it.result) }

        if (pendingResult != null || isSetupComplete) {
            // --- WE ARE IN BATTLE MODE ---
            if (pendingResult != null) {
                currentPhase = GamePhase.BATTLE
                isPlaybackMode = true
                playbackIndex = -1
            } else {
                isPlaybackMode = false
                currentPhase = if (isPassAndPlay) GamePhase.HANDOFF else GamePhase.BATTLE
            }

            syncTabWithTurn(combatMoves)

            if (isBotGame && pendingResult == null && !isOffenseMode) {
                isActionLocked = true
                executeBotTurnLoop(context)
            }
        } else {
            // --- WE ARE IN DEPLOYMENT MODE ---
            if (isPassAndPlay) {
                if (p1PlacedCount >= 17) {
                    // Player 1's ships are in the database. Player 2 needs to setup.
                    isP1SetupPending = false
                    isP2SetupPending = true
                    activePlayer = 2
                } else {
                    // No ships in the database. Player 1 needs to setup.
                    isP1SetupPending = true
                    isP2SetupPending = true
                    activePlayer = 1
                }
                currentPhase = GamePhase.HANDOFF
            } else {
                // Standard Bot/Companion setup
                isP1SetupPending = false
                isP2SetupPending = false
                currentPhase = GamePhase.PLACEMENT
            }

            // This safely locks the UI to the Defense tab
            isOffenseMode = false
        }
        observeCurrentGameMoves(gameId)
    }

    fun updateShipPlacement(index: Int, col: Float, row: Float, isVertical: Boolean) {
        val ship = placementFleet[index]
        val c = col.roundToInt()
        val r = row.roundToInt()

        if (r >= 10) {
            val defaultX = when(ship.name) { "Carrier", "Cruiser" -> 0f; "Battleship" -> 6f; "Submarine" -> 4f; else -> 8f }
            val defaultY = when(ship.name) { "Carrier", "Battleship" -> 11f; else -> 13f }
            placementFleet[index] = ship.copy(x = defaultX, y = defaultY, isVertical = false, isPlaced = false)
            return
        }

        if ((isVertical && r + ship.size > 10) || (!isVertical && c + ship.size > 10) || c < 0 || r < 0) return

        val proposedCells = (0 until ship.size).map { if (isVertical) Pair(c, r + it) else Pair(c + it, r) }
        val otherCells = placementFleet.filterIndexed { i, s -> i != index && s.isPlaced }.flatMap { it.getCells() }

        if (proposedCells.none { it in otherCells }) {
            placementFleet[index] = ship.copy(x = c.toFloat(), y = r.toFloat(), isVertical = isVertical, isPlaced = true)
        }
    }

    // ADD context to the signature
    fun confirmShipPlacement(context: Context) {
        if (isActionLocked) return
        val gameId = currentGameId ?: return
        if (placementFleet.any { !it.isPlaced }) return
        val allCells = placementFleet.flatMap { it.getCells() }
        if (allCells.distinct().size < 17) return

        isActionLocked = true
        if (currentPhase == GamePhase.OPPONENT_SHIPS) {
            // PASS the context down to the save function
            saveOpponentShipsAndFinish(context)
            return
        }

        viewModelScope.launch {
            val targetOffense = activePlayer == 2
            val shipMoves = placementFleet.flatMap { ship ->
                ship.getCells().map { cell ->
                    Move(gameId = gameId, isOffense = targetOffense, x = cell.first, y = cell.second, result = ship.name, turnNumber = 0, shotNumber = 0)
                }
            }
            dao.insertMoves(shipMoves)

            if (isP2SetupPending) {
                currentPhase = GamePhase.HANDOFF
            } else if (currentGame?.gameMode == "PassAndPlay" && currentPhase == GamePhase.PLACEMENT) {
                currentPhase = GamePhase.HANDOFF
                activePlayer = 1
            } else {
                currentPhase = GamePhase.BATTLE
                isOffenseMode = true
            }
            isActionLocked = false
        }
    }

    private fun syncTabWithTurn(combatMoves: List<Move>) {
        if (currentGame?.gameMode == "PassAndPlay") {
            isOffenseMode = true
            val lastMove = combatMoves.lastOrNull()
            if (lastMove == null) {
                // No moves fired yet; Player 1 starts the battle
                activePlayer = 1
            } else {
                if (lastMove.result == "HIT") {
                    // The player who scored a hit keeps their turn
                    activePlayer = if (lastMove.isOffense) 1 else 2
                } else {
                    // It was a MISS, meaning the turn belongs to the other player now
                    activePlayer = if (lastMove.isOffense) 2 else 1
                }
            }
            return
        }

        // Standard turn sync for Bot and Companion modes
        if (combatMoves.isEmpty()) {
            isOffenseMode = true
            return
        }
        val lastMove = combatMoves.last()
        isOffenseMode = if (lastMove.result == "MISS") !lastMove.isOffense else lastMove.isOffense
    }

    private fun getActiveCompanionTurn(): Boolean? {
        val combatMoves = _currentMoves.value.filter { !isShipData(it.result) }
        val lastMove = combatMoves.lastOrNull() ?: return null
        return if (lastMove.result == "MISS") !lastMove.isOffense else lastMove.isOffense
    }

    fun completeHandoff() {
        if (isP1SetupPending) {
            isP1SetupPending = false
            activePlayer = 1
            currentPhase = GamePhase.PLACEMENT
            isOffenseMode = false
            initPlacementFleet()
        } else if (isP2SetupPending) {
            isP2SetupPending = false
            activePlayer = 2
            currentPhase = GamePhase.PLACEMENT
            isOffenseMode = false
            initPlacementFleet()
        } else {
            // REMOVED the activePlayer flip! It is already correct.
            isOffenseMode = true
            currentPhase = GamePhase.BATTLE
            isActionLocked = false
        }
    }

    private fun observeCurrentGameMoves(gameId: Int) {
        movesJob?.cancel()
        movesJob = viewModelScope.launch { dao.getMovesForGame(gameId).collect { _currentMoves.value = it } }
    }

    fun recordMove(x: Int, y: Int, result: String, isSunk: Boolean, context: Context, autoSwitchTurn: Boolean = true) {
        viewModelScope.launch { recordMoveSuspended(x, y, result, isSunk, context, autoSwitchTurn) }
    }

    private suspend fun recordMoveSuspended(x: Int, y: Int, result: String, isSunk: Boolean, context: Context, autoSwitchTurn: Boolean) {
        val gameId = currentGameId ?: return
        val currentList = _currentMoves.value

        val validShots = currentList.filter { !isShipData(it.result) }
        val nextShotNumber = validShots.size + 1
        var calculatedTurn = 1

        if (validShots.isNotEmpty()) {
            var currentOwner = validShots[0].isOffense
            for (i in 1 until validShots.size) {
                if (validShots[i].isOffense != currentOwner) {
                    calculatedTurn++
                    currentOwner = validShots[i].isOffense
                }
            }
            val targetOffense = if (activePlayer == 2) !isOffenseMode else isOffenseMode
            if (targetOffense != currentOwner) calculatedTurn++
        }

        val targetOffense = if (activePlayer == 2) !isOffenseMode else isOffenseMode

        if (currentGame?.gameMode == "Companion") {
            val requiredTurn = getActiveCompanionTurn()
            if (requiredTurn != null && targetOffense != requiredTurn) return
        }

        val move = Move(
            gameId = gameId, isOffense = targetOffense, x = x, y = y,
            result = result, isSunk = isSunk, turnNumber = calculatedTurn, shotNumber = nextShotNumber
        )
        dao.insertMove(move)
        val simulatedList = currentList + move
        _currentMoves.value = simulatedList

        val myDbOffense = activePlayer != 2
        widgetIsPlayerFiring = (targetOffense == myDbOffense)
        widgetShotOutcome = when {
            isSunk -> "SUNK"
            result == "HIT" -> "HIT"
            else -> "MISS"
        }
        widgetShotTriggerKey++

        val isBotGame = currentGame?.gameMode == "Bot"
        val isCompanion = currentGame?.gameMode == "Companion"
        val isPassAndPlay = currentGame?.gameMode == "PassAndPlay"

        if (result == "MISS" && autoSwitchTurn) {
            if (isBotGame || isCompanion) isOffenseMode = !isOffenseMode
            else if (isPassAndPlay) {
                delay(1200)
                // NEW: Shift the player immediately!
                activePlayer = if (activePlayer == 1) 2 else 1
                currentPhase = GamePhase.HANDOFF
            }
        }

        val offenseHits = simulatedList.filter { it.isOffense && it.result == "HIT" }
        val defenseHits = simulatedList.count { !it.isOffense && it.result == "HIT" }

        if (offenseHits.size >= 17 || defenseHits >= 17) {
            val isWin = offenseHits.size >= 17
            pendingResult = if (isWin) "WIN" else "LOSS"

            currentGame?.let { activeGame ->
                val updatedGame = activeGame.copy(result = pendingResult)
                dao.updateGame(updatedGame)
                currentGame = updatedGame
            }

            if (isBotGame) {
                isOffenseMode = true
            } else if (isCompanion) {
                Toast.makeText(context, if (isWin) "Victory! Verify ships now..." else "Defeat! Verify ships now...", Toast.LENGTH_LONG).show()
                val hitCoords = offenseHits.map { Pair(it.x, it.y) }
                val reconstructed = if (isWin) guessFleetFromAnonymousCoords(hitCoords) else null
                startOpponentShipPhase(isWin, reconstructed)
            }
        }
    }

    fun undoLastMove() {
        val gameId = currentGameId ?: return
        viewModelScope.launch {
            val validMoves = _currentMoves.value.filter { !isShipData(it.result) }
            if (validMoves.isEmpty()) return@launch

            if (currentGame?.gameMode == "Bot") {
                val lastHumanMoveIndex = validMoves.indexOfLast { it.isOffense }
                if (lastHumanMoveIndex != -1) {
                    val movesToDelete = validMoves.subList(lastHumanMoveIndex, validMoves.size)
                    dao.deleteMoves(movesToDelete)
                    val remainingMoves = validMoves.subList(0, lastHumanMoveIndex)
                    syncTabWithTurn(remainingMoves)
                    isActionLocked = false
                }
            } else {
                val lastMove = validMoves.last()
                dao.deleteLastMove(gameId)
                val remainingMoves = validMoves.dropLast(1)
                if (currentGame?.gameMode == "PassAndPlay" && lastMove.result == "MISS") {
                    currentPhase = GamePhase.HANDOFF
                } else {
                    syncTabWithTurn(remainingMoves)
                }
            }

            if (currentGame?.result != null || pendingResult != null) {
                pendingResult = null
                currentGame?.let { activeGame ->
                    val reopenedGame = activeGame.copy(result = null)
                    dao.updateGame(reopenedGame)
                    currentGame = reopenedGame
                }
            }
        }
    }

    fun startOpponentShipPhase(manualWin: Boolean? = null, prefilledShips: List<Ship>? = null) {
        if (manualWin != null) pendingResult = if (manualWin) "WIN" else "LOSS"
        currentPhase = GamePhase.OPPONENT_SHIPS
        isOffenseMode = true
        initPlacementFleet()
        widgetShotTriggerKey = 0

        if (pendingResult == "WIN") {
            val coordsToUse = _currentMoves.value.filter { it.isOffense && it.result == "HIT" }.map { Pair(it.x, it.y) }
            val reconstructed = prefilledShips ?: guessFleetFromAnonymousCoords(coordsToUse)
            for (i in placementFleet.indices) {
                val found = reconstructed.find { it.name == placementFleet[i].name }
                if (found != null) placementFleet[i] = found
            }
        }
    }

    fun saveOpponentShipsAndFinish(context: Context) {
        val gameId = currentGameId ?: return
        viewModelScope.launch {
            val existingDbMoves = dao.getMovesForGameSync(gameId)
            val oldShipMoves = existingDbMoves.filter { it.isOffense && isShipData(it.result) }
            if (oldShipMoves.isNotEmpty()) {
                dao.deleteMoves(oldShipMoves)
            }

            val oppShipMoves = placementFleet.filter { it.isPlaced }.flatMap { ship ->
                ship.getCells().map { cell ->
                    Move(gameId = gameId, isOffense = true, x = cell.first, y = cell.second, result = ship.name, turnNumber = 999, shotNumber = 999)
                }
            }
            if (oppShipMoves.isNotEmpty()) dao.insertMoves(oppShipMoves)

            val finalResult = pendingResult ?: if (_currentMoves.value.count { it.isOffense && it.result == "HIT" } >= 17) "WIN" else "LOSS"

            currentGame?.let { activeGame ->
                // THE FIX: Do NOT overwrite botBrainMetadata here. Keep the snapshot taken at startGame!
                val updatedGame = activeGame.copy(result = finalResult)
                dao.updateGame(updatedGame)
                currentGame = updatedGame
                widgetShotTriggerKey = 0
                currentPhase = GamePhase.BATTLE
            }

            observeCurrentGameMoves(gameId)
        }
    }

    fun skipOpponentShips() {
        val gameId = currentGameId
        currentPhase = GamePhase.BATTLE
        isOffenseMode = true

        // If they skipped the edit, restore the memory we temporarily wiped
        if (gameId != null) {
            observeCurrentGameMoves(gameId)
        }
    }

    fun fireAtBot(x: Int, y: Int, context: Context) {
        if (isActionLocked || currentGame?.result != null || !isOffenseMode) return

        val alreadyShot = _currentMoves.value.any { it.isOffense && it.x == x && it.y == y && !isShipData(it.result) }
        if (alreadyShot) {
            Toast.makeText(context, "Target already engaged!", Toast.LENGTH_SHORT).show()
            return
        }

        isActionLocked = true
        val hitShip = botSecretFleet.find { ship -> ship.getCells().contains(Pair(x, y)) }
        val isHit = hitShip != null
        val resultString = if (isHit) "HIT" else "MISS"

        viewModelScope.launch {
            val currentOffenseMoves = _currentMoves.value.filter { it.isOffense }
            val hitCellsCount = hitShip?.getCells()?.count { cell ->
                cell == Pair(x, y) || currentOffenseMoves.any { m -> m.x == cell.first && m.y == cell.second && m.result == "HIT" }
            } ?: 0
            val isSunk = isHit && hitCellsCount == hitShip?.size

            recordMoveSuspended(x, y, resultString, isSunk, context, autoSwitchTurn = false)

            if (currentGame?.result == null) {
                if (!isHit) {
                    isOffenseMode = false
                    executeBotTurnLoop(context)
                } else {
                    isActionLocked = false
                }
            } else {
                isActionLocked = false
            }
        }
    }

    fun firePassAndPlay(x: Int, y: Int, context: Context) {
        if (isActionLocked || currentGame?.result != null || !isOffenseMode) return

        val targetOffense = if (activePlayer == 2) !isOffenseMode else isOffenseMode
        val alreadyShot = _currentMoves.value.any { it.isOffense == targetOffense && it.x == x && it.y == y && !isShipData(it.result) }

        if (alreadyShot) {
            Toast.makeText(context, "Target already engaged!", Toast.LENGTH_SHORT).show()
            return
        }

        isActionLocked = true
        val oppDbOffense = if (activePlayer == 1) true else false
        val oppShipMoves = _currentMoves.value.filter { it.isOffense == oppDbOffense && isShipData(it.result) }
        val oppFleet = reconstructFleetFromMoves(oppShipMoves)

        val hitShip = oppFleet.find { ship -> ship.getCells().contains(Pair(x, y)) }
        val isHit = hitShip != null
        val resultString = if (isHit) "HIT" else "MISS"

        viewModelScope.launch {
            val currentOffenseMoves = _currentMoves.value.filter { it.isOffense == targetOffense && !isShipData(it.result) }
            val hitCellsCount = hitShip?.getCells()?.count { cell ->
                cell == Pair(x, y) || currentOffenseMoves.any { m -> m.x == cell.first && m.y == cell.second && m.result == "HIT" }
            } ?: 0
            val isSunk = isHit && hitCellsCount == hitShip?.size

            recordMoveSuspended(x, y, resultString, isSunk, context, autoSwitchTurn = true)

            if (isHit || currentGame?.result != null) {
                isActionLocked = false
            }
        }
    }

    // --- DELEGATE TO TACTICAL ENGINE ---
    private fun executeBotTurnLoop(context: Context) {
        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            if (currentGame?.result != null) return@launch

            val startTime = System.currentTimeMillis()
            val botMovesSoFar = _currentMoves.value.filter { !it.isOffense && (it.result == "HIT" || it.result == "MISS") }

            // UPGRADED: Pass the human player's explicit name into the router
            // Inside BattleshipViewModel.kt -> executeBotTurnLoop
            val decision = TacticalEngine.getBestMove(
                opponentName = currentGame?.opponentName ?: "Unknown",
                playerName = currentGame?.playerName ?: "Player 1",
                botMovesSoFar = botMovesSoFar,
                context = context,
                gameId = currentGameId ?: 0,
                adlerOffensivePrior = adlerOffensiveMatrix,
                moriartyPrior = moriartyPriorMatrix // <-- NEW
            )

            lastBotDecision = decision.log
            lastBotHeatmap = decision.heatMap
            lastBotDiagnosticMap = decision.diagnosticMap

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1000) delay(1000 - elapsed)

            val playerShipHit = placementFleet.find { ship -> ship.getCells().contains(decision.coordinate) }
            val botIsHit = playerShipHit != null
            val botResultString = if (botIsHit) "HIT" else "MISS"

            val botHitCellsCount = playerShipHit?.getCells()?.count { cell ->
                cell == decision.coordinate || _currentMoves.value.any { m -> !m.isOffense && m.x == cell.first && m.y == cell.second && m.result == "HIT" }
            } ?: 0
            val botIsSunk = botIsHit && botHitCellsCount == playerShipHit?.size

            recordMoveSuspended(decision.coordinate.first, decision.coordinate.second, botResultString, botIsSunk, context, autoSwitchTurn = false)

            if (currentGame?.result == null) {
                if (botIsHit) executeBotTurnLoop(context)
                else {
                    delay(800)
                    isOffenseMode = true
                    isActionLocked = false
                }
            } else {
                isActionLocked = false
            }
        }
    }

    private suspend fun saveBotFleetToDatabaseInternal() {
        val gId = currentGameId ?: return
        val botMoves = botSecretFleet.flatMap { ship ->
            ship.getCells().map { cell ->
                Move(gameId = gId, x = cell.first, y = cell.second, result = ship.name, isOffense = true, isSunk = false, turnNumber = 0, shotNumber = 0)
            }
        }
        if (botMoves.isNotEmpty()) dao.insertMoves(botMoves)
        observeCurrentGameMoves(gId)
    }

    fun renameGame(gameId: Int, newName: String) { viewModelScope.launch { val game = dao.getGameById(gameId); if (game != null) dao.updateGame(game.copy(opponentName = newName)) } }
    fun renamePlayerInGame(gameId: Int, newPlayerName: String) { viewModelScope.launch { val game = dao.getGameById(gameId); if (game != null) dao.updateGame(game.copy(playerName = newPlayerName)) } }
    fun editOpponentShips(gameId: Int, context: Context) {
        viewModelScope.launch {
            loadGameSuspended(gameId, context)

            val oldShipMoves = _currentMoves.value.filter { it.isOffense && isShipData(it.result) }
            if (oldShipMoves.isNotEmpty()) {
                // ONLY remove from memory, DO NOT delete from DAO yet!
                // This protects the database if the user hits the back arrow to cancel.
                _currentMoves.value = _currentMoves.value.filterNot { it in oldShipMoves }
            }

            startOpponentShipPhase(manualWin = true)
        }
    }
    fun deleteGame(gameId: Int) { viewModelScope.launch { dao.deleteGameById(gameId) } }

    fun pauseGame() {
        botTurnJob?.cancel()
        currentGameId = null
        currentGame = null
        movesJob?.cancel()
        _currentMoves.value = emptyList()

        isActionLocked = false
        botSecretFleet = emptyList()
        isP1SetupPending = false
        isP2SetupPending = false
        widgetShotTriggerKey = 0

        // THE FIX: Reset debug toggles when leaving the game
        isHeatmapVisible = false
        isHeatmapNumbersVisible = false
        isDiagnosticVisible = false
    }

    fun setMode(offense: Boolean) {
        if (isActionLocked) return
        if (currentPhase == GamePhase.BATTLE || currentPhase == GamePhase.OPPONENT_SHIPS) isOffenseMode = offense
    }

    suspend fun generateAllGamesCsv(): String {
        val games = dao.getAllGamesSync()
        val allMoves = dao.getAllMovesSync()
        val csvBuilder = StringBuilder()
        csvBuilder.append("GameID,MatchOutcome,GameMode,Player,Opponent,Timestamp,TurnNumber,ShotNumber,Grid,X,Y,Coordinate,ShotResult\n")

        games.forEach { game ->
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val gameMoves = allMoves.filter { it.gameId == game.id }.sortedWith(compareBy({ it.turnNumber }, { it.shotNumber }))
            val timeStr = dateFormatter.format(Date(game.timestamp))
            val offFleet = reconstructFleetFromMoves(gameMoves.filter { it.isOffense })
            val defFleet = reconstructFleetFromMoves(gameMoves.filter { !it.isOffense })

            gameMoves.forEach { move ->
                val grid = if (move.isOffense) "Offense" else "Defense"
                val coordinate = "${(move.y + 65).toChar()}${move.x + 1}"
                var finalResultStr = move.result

                if (isShipData(finalResultStr)) {
                    val fleetToCheck = if (move.isOffense) offFleet else defFleet
                    finalResultStr = fleetToCheck.find { it.getCells().contains(Pair(move.x, move.y)) }?.name ?: "SHIP"
                } else if (move.isSunk) {
                    finalResultStr = "SUNK"
                }
                csvBuilder.append("${game.id},${game.result},${game.gameMode},${game.playerName},${game.opponentName},$timeStr,${move.turnNumber},${move.shotNumber},$grid,${move.x},${move.y},$coordinate,$finalResultStr\n")
            }
        }
        return csvBuilder.toString()
    }

    fun processCsvImport(context: Context, csvText: String) {
        viewModelScope.launch {
            val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty() || !lines[0].contains("GameID")) {
                Toast.makeText(context, "Invalid CSV data", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val isLegacyFormat = !lines[0].contains("ShotNumber", ignoreCase = true)
            val gamesMap = mutableMapOf<Int, Game>()
            val movesMap = mutableMapOf<Int, MutableList<Move>>()

            for (i in 1 until lines.size) {
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                val tokens = lines[i].split(",")
                if (tokens.size < 11) continue

                val oldId = tokens[0].toIntOrNull() ?: continue
                val matchOutcome = tokens[1].ifEmpty { null }

                val isV9Modern = tokens.size == 12
                val isV10NextGen = tokens.size >= 13

                val gameMode: String
                val player: String
                val opponent: String
                val timestampRaw: String
                val turnNumber: Int
                val shotNumber: Int
                val isOffense: Boolean
                val x: Int
                val y: Int
                val rawResult: String

                if (isV10NextGen) {
                    gameMode = tokens[2]
                    player = tokens[3]
                    opponent = tokens[4]
                    timestampRaw = tokens[5]
                    turnNumber = tokens[6].toIntOrNull() ?: 0
                    shotNumber = tokens[7].toIntOrNull() ?: 0
                    isOffense = tokens[8] == "Offense"
                    x = tokens[9].toIntOrNull() ?: 0
                    y = tokens[10].toIntOrNull() ?: 0
                    rawResult = tokens[12]
                } else {
                    val legacyPlayer = tokens[2]
                    val legacyOpponent = tokens[3]
                    timestampRaw = tokens[4]
                    gameMode = when {
                        legacyOpponent.contains("Bot", ignoreCase = true) -> "Bot"
                        legacyPlayer.contains("PassAndPlay", ignoreCase = true) -> "PassAndPlay"
                        else -> "Companion"
                    }
                    player = if (legacyPlayer in listOf("PassAndPlay", "Companion", "Human")) "Player 1" else legacyPlayer
                    opponent = if (gameMode == "PassAndPlay" && legacyOpponent == "PassAndPlay") "Player 2" else legacyOpponent

                    if (isV9Modern) {
                        turnNumber = tokens[5].toIntOrNull() ?: 0
                        shotNumber = tokens[6].toIntOrNull() ?: 0
                        isOffense = tokens[7] == "Offense"
                        x = tokens[8].toIntOrNull() ?: 0
                        y = tokens[9].toIntOrNull() ?: 0
                        rawResult = tokens[11]
                    } else {
                        val rawChronologicalNum = tokens[5].toIntOrNull() ?: 0
                        isOffense = tokens[6] == "Offense"
                        x = tokens[7].toIntOrNull() ?: 0
                        y = tokens[8].toIntOrNull() ?: 0
                        rawResult = tokens[10]
                        val isShip = rawResult == "SHIP" || rawResult in listOf("Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer")
                        shotNumber = if (isShip) 0 else rawChronologicalNum
                        turnNumber = 0
                    }
                }

                val timestamp = try { dateFormatter.parse(timestampRaw)?.time ?: System.currentTimeMillis() }
                catch (e: Exception) { System.currentTimeMillis() }

                if (!gamesMap.containsKey(oldId)) {
                    gamesMap[oldId] = Game(id = 0, playerName = player, opponentName = opponent, gameMode = gameMode, timestamp = timestamp, result = matchOutcome)
                }

                val isSunk = (rawResult == "SUNK")
                val dbResult = if (isSunk) "HIT" else rawResult

                if (!movesMap.containsKey(oldId)) movesMap[oldId] = mutableListOf()
                movesMap[oldId]?.add(Move(gameId = 0, isOffense = isOffense, x = x, y = y, result = dbResult, isSunk = isSunk, turnNumber = turnNumber, shotNumber = shotNumber))
            }

            val existingGames = dao.getAllGamesSync()
            var importedCount = 0

            gamesMap.forEach { (oldId, gameDraft) ->
                if (existingGames.none { it.timestamp == gameDraft.timestamp }) {
                    var gameMoves = movesMap[oldId]?.toList() ?: emptyList()

                    if (isLegacyFormat) {
                        val recalculatedMoves = mutableListOf<Move>()
                        val combatMoves = gameMoves.filter { !isShipData(it.result) }.sortedBy { it.shotNumber }
                        val shipMoves = gameMoves.filter { isShipData(it.result) }.map { it.copy(turnNumber = 0) }

                        var currentGroupedTurn = 1
                        if (combatMoves.isNotEmpty()) {
                            var currentOwner = combatMoves[0].isOffense
                            combatMoves.forEach { move ->
                                if (move.isOffense != currentOwner) {
                                    currentGroupedTurn++
                                    currentOwner = move.isOffense
                                }
                                recalculatedMoves.add(move.copy(turnNumber = currentGroupedTurn))
                            }
                        }
                        gameMoves = shipMoves + recalculatedMoves
                    }

                    val newGameId = dao.insertGame(gameDraft).toInt()
                    dao.insertMoves(gameMoves.map { it.copy(gameId = newGameId) })
                    importedCount++
                }
            }

            Toast.makeText(context, "Imported $importedCount new games", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSmartImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileContent = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                } ?: return@launch

                if (fileContent.trim().startsWith("{") || fileContent.trim().startsWith("[")) {
                    println("Successfully imported AI weights! Size: ${fileContent.length} chars")
                    Toast.makeText(context, "AI Brain Imported!", Toast.LENGTH_SHORT).show()
                } else if (fileContent.contains("GameID")) {
                    processCsvImport(context, fileContent)
                } else {
                    Toast.makeText(context, "Unrecognized file format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // ADLER: ADVANCED MATRIX BUILDER
    // ==========================================

    private fun serializeMetadata(metadata: Map<String, String>): String {
        // Simple key:value;key:value format (no colons/semicolons in values)
        return metadata.entries.joinToString("|") { "${it.key}#${it.value}" }
    }

    private fun parseMetadata(data: String): Map<String, String> {
        return data.split("|").associate {
            val parts = it.split("#", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }
    }

    private fun serializeMatrix(matrix: Array<FloatArray>): String {
        return matrix.joinToString(";") { row -> row.joinToString(",") }
    }

    private fun deserializeMatrix(data: String): Array<FloatArray> {
        val rows = data.split(";")
        return Array(10) { i ->
            if (i < rows.size && rows[i].isNotBlank()) {
                val floats = rows[i].split(",")
                FloatArray(10) { j -> if (j < floats.size) floats[j].toFloatOrNull() ?: 1.0f else 1.0f }
            } else {
                FloatArray(10) { 1.0f }
            }
        }
    }

    private fun calculatePriorsForPlayer(
        human: String,
        allGames: List<Game>,
        allMoves: List<Move>,
        limitTimestamp: Long? = null
    ): Pair<Array<FloatArray>, Array<FloatArray>> {
        val generalContexts = mutableListOf<HumanMatchContext>()
        val specificContexts = mutableListOf<HumanMatchContext>()

        val humanGames = allGames.filter {
            (it.playerName == human || it.opponentName == human) &&
                    (limitTimestamp == null || it.timestamp < limitTimestamp)
        }

        // THE FIX: Group all moves by GameID into a Hash Map (Takes milliseconds)
        val movesByGameId = allMoves.groupBy { it.gameId }

        for (game in humanGames) {
            // THE FIX: Instantly look up the game's moves using the dictionary key!
            val gameMoves = movesByGameId[game.id] ?: emptyList()

            if (gameMoves.isEmpty()) continue

            // Determine which seat the human was sitting in
            val isP1 = game.playerName == human
            val isP2 = game.opponentName == human

            // Extract the human's exact ships
            val humanShips = gameMoves.filter { move ->
                isShipData(move.result) && ((isP1 && !move.isOffense) || (isP2 && move.isOffense))
            }

            // Extract the human's exact combat shots
            val humanShots = gameMoves.filter { move ->
                !isShipData(move.result) && ((isP1 && move.isOffense) || (isP2 && !move.isOffense))
            }.sortedBy { it.shotNumber }

            // Isolate the "Hunt Shots" (Shots before their first HIT)
            val firstHitIndex = humanShots.indexOfFirst { it.result == "HIT" || it.isSunk }
            val huntShots = if (firstHitIndex == -1) humanShots else humanShots.subList(0, firstHitIndex)

            // --- THE FIX: Extract the human's win/loss outcome ---
            val humanWon = (isP1 && game.result == "WIN") || (isP2 && game.result == "LOSS")

            val matchContext = HumanMatchContext(humanShips, huntShots, humanWon)

            // Route to General vs Specific Adler context
            if (game.opponentName == "AdlerBot" || game.playerName == "AdlerBot") {
                specificContexts.add(matchContext)
            } else {
                generalContexts.add(matchContext)
            }
        }

        // 3. Build the raw matrices
        val generalOffense = buildOffensivePrior(generalContexts)
        val specificOffense = buildOffensivePrior(specificContexts)
        val generalDefense = buildDefensivePrior(generalContexts)
        val specificDefense = buildDefensivePrior(specificContexts)

        // 4. Merge the contexts! (70% Specific / 30% General)
        val finalOffense = mergePriors(generalOffense, specificOffense, 0.3f, 0.7f)
        val finalDefense = mergePriors(generalDefense, specificDefense, 0.3f, 0.7f)

        return Pair(finalOffense, finalDefense)
    }

    private fun mergePriors(general: Array<FloatArray>, specific: Array<FloatArray>, generalWeight: Float, specificWeight: Float): Array<FloatArray> {
        val merged = Array(10) { FloatArray(10) { 1.0f } }
        val hasSpecificData = specific.flatMap { it.toList() }.any { it != 1.0f && it != 0.0f }

        for (x in 0..9) {
            for (y in 0..9) {
                if (hasSpecificData) {
                    merged[x][y] = (general[x][y] * generalWeight) + (specific[x][y] * specificWeight)
                } else {
                    merged[x][y] = general[x][y]
                }
            }
        }
        return merged
    }

    private fun buildOffensivePrior(history: List<HumanMatchContext>): Array<FloatArray> {
        val prior = Array(10) { FloatArray(10) { 1.0f } } // Base 1.0 Laplace Smoothing

        // Pure Bayesian: Decaying historical frequency
        val decayFactor = 0.85f // Dropped to 0.85 to help it forget slightly faster
        var currentWeight = 1.0f
        var totalWeightedSamples = 0f

        // Iterate backwards (most recent game first)
        history.asReversed().forEach { match ->
            for (peg in match.humanShipPegs) {
                if (peg.x in 0..9 && peg.y in 0..9) {
                    prior[peg.x][peg.y] += currentWeight
                }
            }
            totalWeightedSamples += currentWeight
            currentWeight *= decayFactor // The next game back matters 15% less
        }

        // Normalize against the average to create multipliers (e.g., 1.2x, 0.8x)
        if (totalWeightedSamples > 0f) {
            val averageWeight = prior.flatMap { it.toList() }.average().toFloat()
            for (x in 0..9) {
                for (y in 0..9) {
                    prior[x][y] /= averageWeight
                }
            }
        }
        return prior
    }

    private fun buildDefensivePrior(history: List<HumanMatchContext>): Array<FloatArray> {
        val prior = Array(10) { FloatArray(10) { 1.0f } } // Base 1.0 Laplace Smoothing

        // Pure Bayesian: Decaying historical frequency
        val decayFactor = 0.85f
        var currentWeight = 1.0f
        var totalWeightedSamples = 0f

        // Iterate backwards (most recent game first)
        history.asReversed().forEach { match ->
            for (shot in match.humanHuntShots) {
                if (shot.x in 0..9 && shot.y in 0..9) {
                    prior[shot.x][shot.y] += currentWeight
                }
            }
            totalWeightedSamples += currentWeight
            currentWeight *= decayFactor // The next game back matters 15% less
        }

        // Normalize against the average to create multipliers
        if (totalWeightedSamples > 0f) {
            val averageWeight = prior.flatMap { it.toList() }.average().toFloat()
            for (x in 0..9) {
                for (y in 0..9) {
                    prior[x][y] /= averageWeight
                }
            }
        }
        return prior
    }
}

class ViewModelFactory(private val dao: BattleshipDao, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BattleshipViewModel::class.java)) return BattleshipViewModel(dao, context) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}