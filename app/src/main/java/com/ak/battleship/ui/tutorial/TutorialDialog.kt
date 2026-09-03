package com.ak.battleship.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TutorialPage(
    val stepTitle: String,
    val headline: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val cards: List<TutorialCardItem>
)

data class TutorialCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlightColor: Color? = null
)

@Composable
fun TutorialDialog(
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }

    val pages = remember {
        listOf(
            TutorialPage(
                stepTitle = "Step 1 of 5",
                headline = "Choose Your Battlefield",
                description = "Battleship supports three distinct game modes tailored for solo tactical mastery, hot-seat duels, or physical tabletop gaming.",
                icon = Icons.Default.Navigation,
                iconTint = Color(0xFF6750A4),
                cards = listOf(
                    TutorialCardItem(
                        title = "Companion Mode (Physical Board)",
                        subtitle = "Playing on a real plastic Battleship board? Use this app as your digital radar! Log shots, track live win odds, and verify secret fleets.",
                        icon = Icons.Default.Sensors
                    ),
                    TutorialCardItem(
                        title = "Pass & Play (Hot Seat)",
                        subtitle = "Pass the phone between turns to battle a friend sitting right beside you in the club.",
                        icon = Icons.Default.Group
                    ),
                    TutorialCardItem(
                        title = "Bot Match (AI Duel)",
                        subtitle = "Test your mettle against 5 tiers of custom AI—from Watson's pure spatial density to Moriarty's neural networks.",
                        icon = Icons.Default.SmartToy
                    )
                )
            ),
            TutorialPage(
                stepTitle = "Step 2 of 5",
                headline = "Continuous Fire Mechanics",
                description = "This is not traditional single-shot Battleship. Speed, momentum, and kill streaks decide the battle.",
                icon = Icons.Default.FlashOn,
                iconTint = Color(0xFFE65100),
                cards = listOf(
                    TutorialCardItem(
                        title = "🔥 Hit = Keep Firing!",
                        subtitle = "Landing a Hit or Sunk immediately awards you another consecutive turn. String hits together to annihilate enemy formations.",
                        icon = Icons.Default.CheckCircle,
                        highlightColor = Color(0xFF2E7D32)
                    ),
                    TutorialCardItem(
                        title = "🌊 Miss = Turn Passes",
                        subtitle = "Your turn only concludes when a shot hits open water. Keep calm, deduce adjacent coordinates, and protect your streak.",
                        icon = Icons.Default.Close,
                        highlightColor = Color(0xFFC62828)
                    )
                )
            ),
            TutorialPage(
                stepTitle = "Step 3 of 5",
                headline = "Fog of War & Blind Sink",
                description = "Master true naval deduction. Information is withheld until the dust settles.",
                icon = Icons.Default.VisibilityOff,
                iconTint = Color(0xFF00695C),
                cards = listOf(
                    TutorialCardItem(
                        title = "Blind Sink Secrecy",
                        subtitle = "When an enemy vessel goes down, the game marks 'Sunk' but NEVER reveals which ship (Carrier, Battleship, Cruiser, Submarine, or Destroyer) was destroyed.",
                        icon = Icons.Default.QuestionMark
                    ),
                    TutorialCardItem(
                        title = "No Free Re-Marking",
                        subtitle = "Prior hits on a sunken ship remain marked as generic hits. You must maintain spatial tracking to know where unhit ships might hide.",
                        icon = Icons.Default.Radar
                    ),
                    TutorialCardItem(
                        title = "Endgame Verification",
                        subtitle = "Once all 17 ship pegs are destroyed, the full enemy fleet layout is revealed alongside full historical trajectory replays.",
                        icon = Icons.Default.DoneAll
                    )
                )
            ),
            TutorialPage(
                stepTitle = "Step 4 of 5",
                headline = "The Bot Hierarchy",
                description = "Meet the Arthur Conan Doyle AI roster. Each bot possesses distinct mathematical psychology.",
                icon = Icons.Default.Psychology,
                iconTint = Color(0xFF1565C0),
                cards = listOf(
                    TutorialCardItem(
                        title = "Watson (Tier 1: Density)",
                        subtitle = "Maps geometric candidate placements. Fast and dependable, but can suffer from combinatorial ship blocking.",
                        icon = Icons.Default.Calculate
                    ),
                    TutorialCardItem(
                        title = "Sherlock (Tier 1: CSP)",
                        subtitle = "Constraint Satisfaction solver with gravitational sweeps. Guarantees mathematical ceiling on wasted moves.",
                        icon = Icons.Default.Search
                    ),
                    TutorialCardItem(
                        title = "Mycroft (Tier 1: MCMC)",
                        subtitle = "Executes 100,000-step zero-allocation Markov Chain Monte Carlo simulations using 128-bit bitboards.",
                        icon = Icons.Default.Memory
                    ),
                    TutorialCardItem(
                        title = "Adler (Tier 2: Bayesian Profiler)",
                        subtitle = "Builds a psychological profile on human habits. Multiplies human bias vectors to punish predictable layouts.",
                        icon = Icons.Default.Fingerprint
                    ),
                    TutorialCardItem(
                        title = "Moriarty (Tier 3: Neural Network)",
                        subtitle = "7-channel CNN/Transformer predicting non-stationary behavioral reactions and meta-strategies in real time.",
                        icon = Icons.Default.AutoAwesome
                    )
                )
            ),
            TutorialPage(
                stepTitle = "Step 5 of 5",
                headline = "Telemetry & Analytics",
                description = "Leverage institutional-grade analytics to refine your fleet tactics and hunting patterns.",
                icon = Icons.Default.Analytics,
                iconTint = Color(0xFF6A1B9A),
                cards = listOf(
                    TutorialCardItem(
                        title = "Dynamic Win Probability",
                        subtitle = "The top gauge calculates live pairwise Z-Score win odds after every shell fired. Cycle through 7 themes (Wall Street, High Seas, Poker, Illusionist, etc.).",
                        icon = Icons.Default.Speed
                    ),
                    TutorialCardItem(
                        title = "Heatmap Inspection",
                        subtitle = "Open the Analytics Dashboard to see where you frequently deploy ships and where opponents tend to fire.",
                        icon = Icons.Default.GridOn
                    ),
                    TutorialCardItem(
                        title = "Full Replay Mode",
                        subtitle = "Tap any historical game from the Home screen to step forward and backward through the exact match progression.",
                        icon = Icons.Default.History
                    )
                )
            )
        )
    }

    Dialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = pages[currentPage].stepTitle,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = { onDismiss(dontShowAgain) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Tutorial")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slide Content with animated slide transition
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                        } else {
                            slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "TutorialSlideAnimation"
                ) { targetPageIdx ->
                    val page = pages[targetPageIdx]
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Header Icon & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(page.iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = page.iconTint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = page.headline,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // Feature Cards
                        page.cards.forEach { card ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = null,
                                        tint = card.highlightColor ?: MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = card.highlightColor ?: MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = card.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Bottom Pagination & Navigation Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Checkbox for "Don't show again"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = "Don't show automatically on launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Indicator Dots and Prev/Next buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous button
                        if (currentPage > 0) {
                            OutlinedButton(
                                onClick = { currentPage-- },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(72.dp))
                        }

                        // Dot indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pages.indices.forEach { index ->
                                val isSelected = index == currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }

                        // Next or Got it button
                        if (currentPage < pages.size - 1) {
                            Button(
                                onClick = { currentPage++ },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onDismiss(dontShowAgain) },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Let's Play!")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Done",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
