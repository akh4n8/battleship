package com.ak.battleship.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.data.Game
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.ui.home.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PRESENTATION LAYER: Home Dashboard
 * * Acts as the master scaffold for game creation and history management.
 * * Delegates complex UI rendering to the components in the ui/home/ package.
 */
@Composable
fun HomeScreen(viewModel: BattleshipViewModel) {
    val context = LocalContext.current
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

    // --- FILE I/O LAUNCHERS ---
    val exportDateFormatter = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val csvData = viewModel.generateAllGamesCsv()
                    context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(csvData.toByteArray()) }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.handleSmartImport(context, it) }
    }

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
                    viewModel.editOpponentShips(game.id, context)
                    gameToRename = null
                }
            )
        }
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
            onExportClick = { saveLauncher.launch("Battleship_Export_${exportDateFormatter.format(Date())}.csv") },
            onImportClick = { importLauncher.launch("*/*") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // History List
        GameHistoryList(
            games = games,
            listState = listState, // NEW: Pass the state down
            onLoadGame = { gameId -> viewModel.loadGame(gameId, context) },
            onRenameGame = { gameToRename = it },
            onDeleteGame = { gameToDelete = it }
        )
    }
}