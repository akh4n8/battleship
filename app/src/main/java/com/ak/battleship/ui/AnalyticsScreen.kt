package com.ak.battleship.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.HeatmapData
import com.ak.battleship.model.TrendData
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.ui.analytics.* // Imports your newly decoupled tabs!

/**
 * PRESENTATION LAYER: Analytics Dashboard
 * * Acts as the master scaffold and state-fetcher for the analytics views.
 * * Delegates all complex rendering to modular Tab components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: BattleshipViewModel, onNavigateBack: () -> Unit) {
    val historyPlayerSuggestions by viewModel.historyPlayerSuggestions.collectAsState()
    val historyOpponentSuggestions by viewModel.historyOpponentSuggestions.collectAsState()
    val currentPlayerFilter by viewModel.playerFilter.collectAsState()
    val currentOpponentFilter by viewModel.opponentFilter.collectAsState()

    // --- 1. CHANGE STATSDATA TO NULLABLE ---
    var selectedTab by remember { mutableStateOf(0) }
    var statsData by remember { mutableStateOf<Pair<Map<String, String>, Map<String, String>>?>(null) } // Fixed!
    var heatmapData by remember { mutableStateOf<HeatmapData?>(null) }
    var heatmapSubTab by remember { mutableStateOf(0) }
    var trendData by remember { mutableStateOf<TrendData?>(null) }

    var expandedPlayerFilterMenu by remember { mutableStateOf(false) }
    var expandedOpponentFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentPlayerFilter, currentOpponentFilter, selectedTab) {
        val pFilter = if (currentPlayerFilter == "All") null else currentPlayerFilter
        val oFilter = if (currentOpponentFilter == "All") null else currentOpponentFilter

        // --- 2. CLEAR PREVIOUS DATA TO TRIGGER LOADER ---
        statsData = null
        heatmapData = null
        trendData = null

        // Artificial delay for smooth UX transition
        kotlinx.coroutines.delay(300)

        when (selectedTab) {
            0 -> statsData = viewModel.getStats(pFilter, oFilter)
            1 -> heatmapData = viewModel.fetchHeatmapData(pFilter, oFilter)
            2 -> trendData = viewModel.getTrendData(pFilter, oFilter)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- TOP APP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        // --- CASCADING FILTERS ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(onClick = { expandedPlayerFilterMenu = true }, label = { Text(if (currentPlayerFilter == "All") "Player: All" else "Player: $currentPlayerFilter") })
                DropdownMenu(expanded = expandedPlayerFilterMenu, onDismissRequest = { expandedPlayerFilterMenu = false }) {
                    DropdownMenuItem(text = { Text("Show All Players") }, onClick = { viewModel.setPlayerFilter("All"); expandedPlayerFilterMenu = false })
                    HorizontalDivider()
                    historyPlayerSuggestions.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setPlayerFilter(name); expandedPlayerFilterMenu = false }) }
                }
            }
            Box {
                AssistChip(onClick = { expandedOpponentFilterMenu = true }, label = { Text(if (currentOpponentFilter == "All") "Opponent: All" else "Opp: $currentOpponentFilter") })
                DropdownMenu(expanded = expandedOpponentFilterMenu, onDismissRequest = { expandedOpponentFilterMenu = false }) {

                    val humans = historyOpponentSuggestions.filterNot { it.contains("Bot", ignoreCase = true) }
                    val bots = historyOpponentSuggestions.filter { it.contains("Bot", ignoreCase = true) }

                    DropdownMenuItem(text = { Text("Show All Opponents") }, onClick = { viewModel.setOpponentFilter("All"); expandedOpponentFilterMenu = false })

                    if (humans.isNotEmpty()) {
                        HorizontalDivider()
                        humans.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setOpponentFilter(name); expandedOpponentFilterMenu = false })
                        }
                    }

                    if (bots.isNotEmpty()) {
                        HorizontalDivider()
                        bots.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { viewModel.setOpponentFilter(name); expandedOpponentFilterMenu = false }
                            )
                        }
                    }
                }
            }
        }

        // --- NAVIGATION TABS ---
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Overview", modifier = Modifier.padding(16.dp)) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Heatmaps", modifier = Modifier.padding(16.dp)) }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Trends", modifier = Modifier.padding(16.dp)) }
        }

        // --- CONTENT ROUTING ---
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> {
                    if (statsData == null) SonarLoader("COMPILING METRICS...")
                    else OverviewTab(statsData!!)
                }
                1 -> {
                    // Note: We handle the null check inside HeatmapsTab directly
                    HeatmapsTab(heatmapData, heatmapSubTab) { heatmapSubTab = it }
                }
                2 -> {
                    TrendsTab(trendData)
                }
            }
        }
    }
}