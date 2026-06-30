package com.ak.battleship.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ak.battleship.data.Game

@Composable
fun DeleteGameDialog(
    game: Game,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Game") },
        text = { Text("Are you sure you want to delete this game against ${game.opponentName}?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = Color.Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameGameDialog(
    game: Game,
    onConfirm: (newPlayerName: String, newOpponentName: String) -> Unit,
    onDismiss: () -> Unit,
    onEditFleet: () -> Unit
) {
    var newPlayerName by remember(game) { mutableStateOf(game.playerName) }
    var newOpponentName by remember(game) { mutableStateOf(game.opponentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Match Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Player Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (game.gameMode == "Bot") game.opponentName else newOpponentName,
                    onValueChange = { if (game.gameMode != "Bot") newOpponentName = it },
                    label = { Text(if (game.gameMode == "Bot") "Opponent (Bot - Locked)" else "Opponent Name") },
                    enabled = game.gameMode != "Bot", // Disables the UI interaction
                    modifier = Modifier.fillMaxWidth()
                )

                // THE FIX: Only allow ship re-mapping for Companion games
                if (game.gameMode == "Companion") {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onEditFleet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Re-Map Opponent Fleet", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(newPlayerName, newOpponentName) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

}