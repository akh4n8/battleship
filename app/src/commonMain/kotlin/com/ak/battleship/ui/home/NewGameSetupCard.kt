package com.ak.battleship.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ak.battleship.ai.TensorFlowBridge

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.data.Game
import com.ak.battleship.ui.getPlayerColor
import com.ak.battleship.ui.partitionPlayers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameSetupCard(
    playerSuggestions: List<String>,
    opponentSuggestions: List<String>,
    gamesHistory: List<Game>,
    onStartGame: (mode: String, player: String, opponent: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val modes = listOf("Companion", "PassAndPlay", "Bot")

    var selectedMode by remember { mutableStateOf("Companion") }
    var playerName by remember { mutableStateOf("Your Name") }
    var opponentName by remember { mutableStateOf("") }

    var playerDropdownExpanded by remember { mutableStateOf(false) }
    var opponentDropdownExpanded by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(gamesHistory) {
        if (gamesHistory.isNotEmpty() && !hasInitialized) {
            val absoluteLastGame = gamesHistory.first()
            selectedMode = absoluteLastGame.gameMode
            playerName = absoluteLastGame.playerName
            opponentName = absoluteLastGame.opponentName
            hasInitialized = true
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
        onClick = { if (!isExpanded) isExpanded = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!isExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "New Game", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start New Game", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Text("Configure Match", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                val lastGameForMode = gamesHistory.firstOrNull { it.gameMode == mode }
                                if (lastGameForMode != null) {
                                    playerName = lastGameForMode.playerName
                                    opponentName = lastGameForMode.opponentName
                                } else {
                                    opponentName = if (mode == "Bot") "WatsonBot" else ""
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) { Text(mode, fontSize = 12.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = playerDropdownExpanded, onExpandedChange = { playerDropdownExpanded = it }) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Player Name") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = getPlayerColor(playerName))
                    )

                    val (humans, bots) = remember(playerSuggestions) { partitionPlayers(playerSuggestions) }

                    ExposedDropdownMenu(
                        expanded = playerDropdownExpanded && (humans.isNotEmpty() || bots.isNotEmpty()),
                        onDismissRequest = { playerDropdownExpanded = false }
                    ) {
                        humans.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, color = getPlayerColor(suggestion), fontWeight = FontWeight.Bold) },
                                onClick = { playerName = suggestion; playerDropdownExpanded = false }
                            )
                        }

                        if (humans.isNotEmpty() && bots.isNotEmpty()) {
                            HorizontalDivider()
                        }

                        bots.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, color = getPlayerColor(suggestion), fontWeight = FontWeight.Bold) },
                                onClick = { playerName = suggestion; playerDropdownExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedMode == "Bot") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.weight(1f).height(56.dp), shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                Column {
                                    Text("Opponent Configuration", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = when (opponentName) {
                                            "WatsonBot" -> "Marginal Probability Active"
                                            "SherlockBot" -> "Constraint Solver Active"
                                            "AdlerBot" -> "Psychological Profiler Active"
                                            "MycroftBot" -> "Monte Carlo Engine Active"
                                            "MoriartyBot" -> "Neural Network Sequencer Active"
                                            "HudsonBot" -> "Calculated Serendipity Active"
                                            else -> "AI Framework Initialized"
                                        },
                                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Box {
                            val activeBotColor = getPlayerColor(opponentName)

                            FilledTonalButton(
                                onClick = { opponentDropdownExpanded = true }, shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = activeBotColor.copy(alpha = 0.15f),
                                    contentColor = activeBotColor
                                ),
                                modifier = Modifier.height(56.dp)
                            ) { Text("VS: $opponentName", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                            DropdownMenu(expanded = opponentDropdownExpanded, onDismissRequest = { opponentDropdownExpanded = false }) {
                                DropdownMenuItem(text = { Text("Watson AI (Forgiving)", color = getPlayerColor("WatsonBot"), fontWeight = FontWeight.SemiBold) }, onClick = { opponentName = "WatsonBot"; opponentDropdownExpanded = false })
                                DropdownMenuItem(text = { Text("Sherlock AI (Difficult)", color = getPlayerColor("SherlockBot"), fontWeight = FontWeight.Bold) }, onClick = { opponentName = "SherlockBot"; opponentDropdownExpanded = false })
                                DropdownMenuItem(text = { Text("Mycroft AI (Relentless)", color = getPlayerColor("MycroftBot"), fontWeight = FontWeight.Bold) }, onClick = { opponentName = "MycroftBot"; opponentDropdownExpanded = false })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Adler AI (Exploitative)", color = getPlayerColor("AdlerBot"), fontWeight = FontWeight.Bold) }, onClick = { opponentName = "AdlerBot"; opponentDropdownExpanded = false })
                                if (TensorFlowBridge.isMoriartyAvailable()) {
                                    DropdownMenuItem(text = { Text("Moriarty AI (Omniscient)", color = getPlayerColor("MoriartyBot"), fontWeight = FontWeight.Bold) }, onClick = { opponentName = "MoriartyBot"; opponentDropdownExpanded = false})
                                }
// "Hudson AI (Empowering)
                            }
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(expanded = opponentDropdownExpanded, onExpandedChange = { opponentDropdownExpanded = it }) {
                        OutlinedTextField(
                            value = opponentName,
                            onValueChange = { opponentName = it },
                            label = { Text(if (selectedMode == "PassAndPlay") "Player 2 Name" else "Opponent Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = getPlayerColor(opponentName))
                        )

                        val (humans, bots) = remember(opponentSuggestions) { partitionPlayers(opponentSuggestions) }

                        ExposedDropdownMenu(
                            expanded = opponentDropdownExpanded && (humans.isNotEmpty() || bots.isNotEmpty()),
                            onDismissRequest = { opponentDropdownExpanded = false }
                        ) {
                            humans.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, color = getPlayerColor(suggestion), fontWeight = FontWeight.Bold) },
                                    onClick = { opponentName = suggestion; opponentDropdownExpanded = false }
                                )
                            }

                            if (humans.isNotEmpty() && bots.isNotEmpty()) {
                                HorizontalDivider()
                            }

                            bots.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, color = getPlayerColor(suggestion), fontWeight = FontWeight.Bold) },
                                    onClick = { opponentName = suggestion; opponentDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isExpanded = false }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onStartGame(selectedMode, playerName.ifBlank { "Player 1" }, opponentName.ifBlank { "Unknown" }); isExpanded = false }) { Text("Deploy Fleet") }
                }
            }
        }
    }
}