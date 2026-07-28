package com.ak.battleship.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ak.battleship.model.PlaybackCommand
import com.ak.battleship.model.ShotOutcome
import com.ak.battleship.viewmodel.WidgetTheme
import com.ak.battleship.ui.widgets.* // Imports all your distributed widgets!

@Composable
fun DynamicWinProbabilityWidget(
    probability: Float,
    currentTheme: WidgetTheme,
    onCycleTheme: () -> Unit,
    stats: List<Pair<String, String>>,
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    isPlayerFiring: Boolean = true,
    // --- THE NEW COMMAND PARAMETERS ---
    command: PlaybackCommand = PlaybackCommand.IDLE,
    commandKey: Int = 0
) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
        Crossfade(targetState = currentTheme, label = "ThemeSwitch") { theme ->
            when (theme) {
                WidgetTheme.ICE_FISHING -> SubmersibleCameraWidget(
                    probability = probability,
                    stats = stats,
                    shotOutcome = shotOutcome,
                    isPlayerTurn = isPlayerFiring,
                    command = command,
                    commandKey = commandKey
                )
                WidgetTheme.STANDARD_GAUGE -> StandardGaugeWidget(
                    probability = probability,
                    command = command,
                    commandKey = commandKey
                )
                WidgetTheme.BROADSIDE -> BroadsideNavalWidget(
                    probability = probability,
                    shotOutcome = shotOutcome,
                    commandKey = commandKey,
                    isPlayerTurn = isPlayerFiring,
                    command = command
                )
                WidgetTheme.SYMPHONY_CONDUCTOR -> SymphonyConductorWidget(
                    probability = probability,
                    shotOutcome = shotOutcome,
                    isPlayerTurn = isPlayerFiring,
                    command = command,
                    commandKey = commandKey
                )
                WidgetTheme.WALL_STREET -> WallStreetWidget(probability, stats)
                WidgetTheme.POKER -> HighStakesPokerWidget(probability, shotOutcome, commandKey, isPlayerFiring)
                WidgetTheme.ILLUSIONIST -> IllusionistWidget(probability, shotOutcome, commandKey, isPlayerFiring)

                else -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Theme [${theme.title}] Under Construction...", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.1f))
                .clickable { onCycleTheme() }
                .padding(6.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Switch Theme", modifier = Modifier.size(16.dp), tint = Color.DarkGray)
        }
    }
}
