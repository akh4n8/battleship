package com.ak.battleship.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The canonical Arthur Conan Doyle hierarchy
val BOT_HIERARCHY = listOf(
    "WatsonBot",
    "SherlockBot",
    "MycroftBot",
    "AdlerBot",
    "MoriartyBot",
    "HudsonBot"
)

/**
 * Splits a list of names into two distinct lists:
 * 1. Humans (Alphabetized)
 * 2. Bots (Sorted by the canonical BOT_HIERARCHY)
 */
fun partitionPlayers(names: List<String>): Pair<List<String>, List<String>> {
    val bots = names.filter { BOT_HIERARCHY.contains(it) }.sortedBy { BOT_HIERARCHY.indexOf(it) }
    val humans = names.filterNot { BOT_HIERARCHY.contains(it) }.sortedWith(String.CASE_INSENSITIVE_ORDER)
    return Pair(humans, bots)
}

// Paste your complete getPlayerColor function here
@Composable
fun getPlayerColor(name: String): Color {
    return when (name) {
        "WatsonBot" -> Color(0xFF637E7C.toInt())
        "SherlockBot" -> Color(0xFF0469B2.toInt())
        "MycroftBot" -> Color(0xFFA8760F.toInt())
        "AdlerBot" -> Color(0xFF715673.toInt())
        "MoriartyBot" -> Color(0xFF8B0000.toInt())
        "HudsonBot" -> Color(0xFF2E7D32.toInt())
        else -> MaterialTheme.colorScheme.primary // Default for human names
    }
}