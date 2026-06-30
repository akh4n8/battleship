package com.ak.battleship.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverviewTab(stats: Pair<Map<String, String>, Map<String, String>>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Text("Standard Metrics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(stats.first.toList()) { (title, value) -> StatCard(title, value) }

        item(span = { GridItemSpan(2) }) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Wild Stats", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        items(stats.second.toList()) { (title, value) -> StatCard(title, value) }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    val description = when (title) {
        "Strike Accuracy" -> "Your overall hit percentage"
        "Opp. Accuracy" -> "Opponent's hit percentage"
        "Fastest Victory" -> "Fewest shots taken to win"
        "Fastest Defeat" -> "Fewest shots taken to lose"
        "My Steal Back %" -> "Won after Opp hit first"
        "Opp. Steal Back %" -> "Lost after hitting first"
        "Destroyer Comeback" -> "Won after losing Destroyer"
        "Opp. Dest. Comeback" -> "Lost after sinking Destroyer"
        "Clutch Victories" -> "Won with 1 ship left"
        "Clutch Defeats" -> "Lost to 1 enemy ship"
        "Ultra Clutch Wins" -> "Won when Opp had 16 hits"
        "Ultra Clutch Defeats" -> "Lost when you had 16 hits"
        else -> null
    }

    val valueFontSize = if (value.contains("\n")) 16.sp else 28.sp

    Card(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
            if (description != null) {
                Text(description, fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f), textAlign = TextAlign.Center, lineHeight = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value, fontSize = valueFontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = when {
                    title.contains("Win") || title.contains("Victory") || title.contains("Victories") || title == "Strike Accuracy" || title == "First Strike Hit %" || title == "My Steal Back %" || title == "Destroyer Comeback" -> Color(0xFF388E3C)
                    title.contains("Loss") || title.contains("Defeat") || title.contains("Defeats") || title == "Opp. Accuracy" || title == "Opp. First Str. Hit %" || title == "Opp. Steal Back %" || title == "Opp. Dest. Comeback" -> Color(0xFFD32F2F)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}