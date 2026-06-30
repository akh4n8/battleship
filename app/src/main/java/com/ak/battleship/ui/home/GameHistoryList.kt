package com.ak.battleship.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.data.Game
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState

@Composable
fun HomeFilterRow(
    currentModeFilter: String,
    currentPlayerFilter: String,
    currentOpponentFilter: String,
    currentStatusFilter: String,
    historyPlayerSuggestions: List<String>,
    historyOpponentSuggestions: List<String>,
    onModeFilterChange: (String) -> Unit,
    onPlayerFilterChange: (String) -> Unit,
    onOpponentFilterChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    var expandedModeFilterMenu by remember { mutableStateOf(false) }
    var expandedPlayerFilterMenu by remember { mutableStateOf(false) }
    var expandedOpponentFilterMenu by remember { mutableStateOf(false) }
    var expandedStatusFilterMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onImportClick) { Text("Import", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onExportClick) { Text("Export", fontWeight = FontWeight.Bold) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                // PROACTIVE FIX: Allow the chips to scroll horizontally on small screens
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 0. Game Mode Filter
            Box {
                AssistChip(
                    onClick = { expandedModeFilterMenu = true },
                    label = { Text(if (currentModeFilter == "All") "Mode: All" else currentModeFilter) }
                )
                DropdownMenu(expanded = expandedModeFilterMenu, onDismissRequest = { expandedModeFilterMenu = false }) {
                    DropdownMenuItem(text = { Text("All Modes") }, onClick = { onModeFilterChange("All"); expandedModeFilterMenu = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Bot") }, onClick = { onModeFilterChange("Bot"); expandedModeFilterMenu = false })
                    DropdownMenuItem(text = { Text("PassAndPlay") }, onClick = { onModeFilterChange("PassAndPlay"); expandedModeFilterMenu = false })
                    DropdownMenuItem(text = { Text("Companion") }, onClick = { onModeFilterChange("Companion"); expandedModeFilterMenu = false })
                }
            }

            // 1. Player Filter
            Box {
                AssistChip(onClick = { expandedPlayerFilterMenu = true }, label = { Text(if (currentPlayerFilter == "All") "Player: All" else "Player: $currentPlayerFilter") })
                DropdownMenu(expanded = expandedPlayerFilterMenu, onDismissRequest = { expandedPlayerFilterMenu = false }) {
                    DropdownMenuItem(text = { Text("Show All Players") }, onClick = { onPlayerFilterChange("All"); expandedPlayerFilterMenu = false })
                    HorizontalDivider()
                    historyPlayerSuggestions.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { onPlayerFilterChange(name); expandedPlayerFilterMenu = false }) }
                }
            }

            // 2. Opponent Filter
            Box {
                AssistChip(onClick = { expandedOpponentFilterMenu = true }, label = { Text(if (currentOpponentFilter == "All") "Opponent: All" else "Opp: $currentOpponentFilter") })
                DropdownMenu(expanded = expandedOpponentFilterMenu, onDismissRequest = { expandedOpponentFilterMenu = false }) {

                    val humans = historyOpponentSuggestions.filterNot { it.contains("Bot", ignoreCase = true) }
                    val bots = historyOpponentSuggestions.filter { it.contains("Bot", ignoreCase = true) }

                    DropdownMenuItem(text = { Text("Show All Opponents") }, onClick = { onOpponentFilterChange("All"); expandedOpponentFilterMenu = false })

                    if (humans.isNotEmpty()) {
                        HorizontalDivider()
                        humans.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { onOpponentFilterChange(name); expandedOpponentFilterMenu = false })
                        }
                    }

                    if (bots.isNotEmpty()) {
                        HorizontalDivider()
                        bots.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { onOpponentFilterChange(name); expandedOpponentFilterMenu = false }
                            )
                        }
                    }
                }
            }

            // 3. Status Filter (NEW)
            Box {
                AssistChip(
                    onClick = { expandedStatusFilterMenu = true },
                    label = { Text(if (currentStatusFilter == "All") "Status: All" else currentStatusFilter) }
                )
                DropdownMenu(expanded = expandedStatusFilterMenu, onDismissRequest = { expandedStatusFilterMenu = false }) {
                    DropdownMenuItem(text = { Text("All Games") }, onClick = { onStatusFilterChange("All"); expandedStatusFilterMenu = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Completed") }, onClick = { onStatusFilterChange("Completed"); expandedStatusFilterMenu = false })
                    DropdownMenuItem(text = { Text("In Progress") }, onClick = { onStatusFilterChange("In Progress"); expandedStatusFilterMenu = false })
                }
            }
        }
    }
}

@Composable
fun GameHistoryList(
    games: List<Game>,
    listState: LazyListState, // NEW: Accepts the hoisted scroll state
    onLoadGame: (Int) -> Unit,
    onRenameGame: (Game) -> Unit,
    onDeleteGame: (Game) -> Unit
) {
    // UPDATED: Now includes time of day (e.g., "Jun 23, 2026, 9:22 PM")
    val historyDateFormatter = remember { SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()) }

    LazyColumn(
        state = listState, // NEW: Binds the memory to the list
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        if (games.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No match records found.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        items(games, key = { it.id }) { game -> // ADDED KEY: Helps Compose remember the exact item when scrolling
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onLoadGame(game.id) }) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${game.playerName} vs ${game.opponentName}", fontWeight = FontWeight.Bold)
                        Text("${game.gameMode}  •  ${historyDateFormatter.format(Date(game.timestamp))}", fontSize = 12.sp, color = Color.Gray)
                        if (game.result != null) {
                            Text("Result: ${game.result}", color = if (game.result == "WIN") Color(0xFF388E3C) else Color(0xFFD32F2F), fontSize = 12.sp)
                        } else {
                            Text("Paused / Ongoing", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    // Restore the Edit button for ALL games
                    IconButton(onClick = { onRenameGame(game) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Game")
                    }

                    IconButton(onClick = { onDeleteGame(game) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}