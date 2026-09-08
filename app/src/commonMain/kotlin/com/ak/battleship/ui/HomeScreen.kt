package com.ak.battleship.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.data.Game
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.ui.home.*
import kotlinx.coroutines.launch

/**
 * PRESENTATION LAYER: Home Dashboard
 * * Acts as the master scaffold for game creation and history management.
 * * Delegates complex UI rendering to the components in the ui/home/ package.
 */
@Composable
fun HomeScreen(viewModel: BattleshipViewModel) {
    
    val scope = rememberCoroutineScope()

    // --- STATE OBSERVATION ---
    val games by viewModel.allGames.collectAsState()
    val opponentSuggestions by viewModel.opponentSuggestions.collectAsState()
    val playerSuggestions by viewModel.playerSuggestions.collectAsState()
    val historyOpponentSuggestions by viewModel.historyOpponentSuggestions.collectAsState()
    val historyPlayerSuggestions by viewModel.historyPlayerSuggestions.collectAsState()
    val currentOpponentFilter by viewModel.opponentFilter.collectAsState()
    val currentPlayerFilter by viewModel.playerFilter.collectAsState()
    val currentStatusFilter by viewModel.statusFilter.collectAsState()
    val currentModeFilter by viewModel.gameModeFilter.collectAsState()

    // --- DIALOG STATES ---
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var gameToRename by remember { mutableStateOf<Game?>(null) }

    val onExport = rememberCsvExporter(viewModel, null)
    val onImport = rememberCsvImporter(viewModel, null)

    // --- SCROLL MEMORY INTEGRATION ---
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.historyScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.historyScrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.historyScrollIndex = listState.firstVisibleItemIndex
        viewModel.historyScrollOffset = listState.firstVisibleItemScrollOffset
    }

    // --- DIALOG RENDERING ---
    gameToDelete?.let { game ->
        DeleteGameDialog(game = game, onConfirm = { viewModel.deleteGame(game.id); gameToDelete = null }, onDismiss = { gameToDelete = null })
    }

    gameToRename?.let { game ->
        gameToRename?.let { game ->
            RenameGameDialog(
                game = game,
                onConfirm = { newPlayer, newOpponent ->
                    if (newPlayer != game.playerName && newPlayer.isNotBlank()) viewModel.renamePlayerInGame(game.id, newPlayer)
                    if (newOpponent != game.opponentName && newOpponent.isNotBlank()) viewModel.renameGame(game.id, newOpponent)
                    gameToRename = null
                },
                onDismiss = { gameToRename = null },
                onEditFleet = {
                    viewModel.editOpponentShips(game.id)
                    gameToRename = null
                }
            )
        }
    }


    if (viewModel.showTelemetryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTelemetryDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("AI Research Telemetry", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Help our club train smarter Battleship bots! When enabled, anonymous match data (shot coordinates, hits/misses, and game outcomes) is automatically contributed to our research database when a match finishes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🛡️ 100% Anonymous: We never collect names, IP addresses, locations, or device info.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Contribute Anonymous Match Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = viewModel.telemetryOptIn,
                            onCheckedChange = { viewModel.setTelemetryOptIn(it) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissTelemetryDialog() }) {
                    Text("Got It")
                }
            }
        )
    }

    // --- MAIN LAYOUT ---
    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Battleship", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { viewModel.openTutorial() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "How to Play / Rules",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { viewModel.openTelemetryDialog() }) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Telemetry Settings",
                        tint = if (viewModel.telemetryOptIn) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Button(onClick = { viewModel.openAnalytics() }) { Text("Analytics", fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Game Configuration Card
        NewGameSetupCard(
            playerSuggestions = playerSuggestions,
            opponentSuggestions = opponentSuggestions,
            gamesHistory = games,
            onStartGame = { mode, player, opponent -> viewModel.startGame(mode, player, opponent) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // History Filters & IO Buttons
        HomeFilterRow(
            currentModeFilter = currentModeFilter,
            currentPlayerFilter = currentPlayerFilter,
            currentOpponentFilter = currentOpponentFilter,
            currentStatusFilter = currentStatusFilter, // NEW
            historyPlayerSuggestions = historyPlayerSuggestions,
            historyOpponentSuggestions = historyOpponentSuggestions,
            onModeFilterChange = { viewModel.setGameModeFilter(it) },
            onPlayerFilterChange = { viewModel.setPlayerFilter(it) },
            onOpponentFilterChange = { viewModel.setOpponentFilter(it) },
            onStatusFilterChange = { viewModel.setStatusFilter(it) }, // NEW
            onExportClick = onExport,
            onImportClick = onImport
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // History List
        GameHistoryList(
            games = games,
            listState = listState, // NEW: Pass the state down
            onLoadGame = { gameId -> viewModel.loadGame(gameId) },
            onRenameGame = { gameToRename = it },
            onDeleteGame = { gameToDelete = it }
        )
    }
}