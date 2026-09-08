import re

with open('app/src/commonMain/kotlin/com/ak/battleship/ui/HomeScreen.kt', 'r') as f:
    text = f.read()

# Add dialog state
if 'var showTelemetryDialog by remember { mutableStateOf(false) }' not in text:
    state_block = """    var gameToRename by remember { mutableStateOf<Game?>(null) }
    var showTelemetryDialog by remember { mutableStateOf(false) }"""
    text = text.replace('    var gameToRename by remember { mutableStateOf<Game?>(null) }', state_block)

# Add the Dialog rendering
dialog_ui = """
    if (showTelemetryDialog) {
        AlertDialog(
            onDismissRequest = { showTelemetryDialog = false },
            title = { Text("AI Research Telemetry") },
            text = {
                Column {
                    Text("Help our club train better Battleship bots! By opting in, your game board and shot coordinates will be anonymously uploaded to our Supabase dataset when a match finishes.")
                    Spacer(Modifier.height(16.dp))
                    Text("We do NOT collect IP addresses, real names, or device information.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = viewModel.playerAlias,
                        onValueChange = { viewModel.setPlayerAlias(it) },
                        label = { Text("Your Callsign / Alias") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = viewModel.telemetryOptIn,
                            onCheckedChange = { viewModel.setTelemetryOptIn(it) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Enable Anonymous Telemetry")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTelemetryDialog = false }) { Text("Done") }
            }
        )
    }
"""
if 'if (showTelemetryDialog)' not in text:
    text = text.replace('    // --- MAIN LAYOUT ---', dialog_ui + '\n    // --- MAIN LAYOUT ---')

# Add the Header Button
button_ui = """                IconButton(onClick = { showTelemetryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Telemetry Settings",
                        tint = if (viewModel.telemetryOptIn) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }"""
if 'Icons.Default.CloudUpload' not in text:
    text = text.replace('                Button(onClick = { viewModel.openAnalytics() }) { Text("Analytics", fontWeight = FontWeight.Bold) }',
                        button_ui + '\n                Button(onClick = { viewModel.openAnalytics() }) { Text("Analytics", fontWeight = FontWeight.Bold) }')

# Add missing imports
imports = """import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.graphics.Color
"""
if 'Icons.Default.CloudUpload' not in text:
    pass # Already replaced
text = text.replace('import androidx.compose.material.icons.automirrored.filled.HelpOutline', imports + 'import androidx.compose.material.icons.automirrored.filled.HelpOutline')

with open('app/src/commonMain/kotlin/com/ak/battleship/ui/HomeScreen.kt', 'w') as f:
    f.write(text)
