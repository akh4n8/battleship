package com.ak.battleship.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.TrendData

@Composable
fun TrendsTab(data: TrendData?) {
    if (data == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SonarLoader("ANALYZING TRAJECTORIES...")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Recent Match Momentum", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MomentumBar(data.winLossHistory)
        }

        // --- NEW CHART: TARGET ACQUISITION ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Target Acquisition Speed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Is your opening search algorithm improving?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(data = data.targetAcquisitionHistory, lineColor = Color(0xFF673AB7), suffix = " Shots")
            Text("Shots fired to score first hit (lower is better).", fontSize = 12.sp, color = Color.Gray)
        }

        // --- CHART 1: OFFENSIVE ACCURACY ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Strike Accuracy Progression", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Are your offensive strikes becoming more precise?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(data = data.accuracyHistory, lineColor = Color(0xFF1976D2), suffix = "%")
            Text("Your hit percentage over recent games.", fontSize = 12.sp, color = Color.Gray)
        }

        // --- CHART 2: DEFENSIVE EVASION ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Evasive Maneuvers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Are your defensive placements becoming harder to crack?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(data = data.oppAccuracyHistory, lineColor = Color(0xFFD32F2F), suffix = "%")
            Text("Opponent's hit percentage (lower is better).", fontSize = 12.sp, color = Color.Gray)
        }

        // --- CHART 3: SPEED TO VICTORY ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Speed to Victory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Are your overall tactics ending games faster?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(data = data.timeToSunkHistory, lineColor = Color(0xFF388E3C), suffix = " Shots")
            Text("Shots taken to win (lower is better).", fontSize = 12.sp, color = Color.Gray)
        }

        // --- CHART 4: DOMINANCE ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Damage Absorbed in Victories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Are you dominating matches while taking less damage?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(data = data.damageTakenHistory, lineColor = Color(0xFFF57C00), suffix = " Hits")
            Text("Hits taken before winning (lower is better).", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MomentumBar(results: List<String>) {
    if (results.isEmpty()) {
        Text("Not enough games played to show momentum.", color = Color.Gray, fontSize = 14.sp)
        return
    }

    Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        results.forEach { result ->
            val color = if (result == "WIN") Color(0xFF388E3C) else Color(0xFFD32F2F)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp).background(color, shape = MaterialTheme.shapes.small))
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Older", fontSize = 10.sp, color = Color.Gray)
        Text("Newer", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun TrendLineChart(data: List<Float>, lineColor: Color, suffix: String = "", invertY: Boolean = false) {
    if (data.size < 2) {
        Text("Play at least 2 games to generate trend lines.", color = Color.Gray, fontSize = 14.sp)
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOrNull() ?: 100f
    val minVal = data.minOrNull() ?: 0f
    val range = if (maxVal == minVal) 1f else maxVal - minVal

    Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFF5F5F5), MaterialTheme.shapes.medium)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = size.width
            val height = size.height
            val xStep = width / (data.size - 1)

            // --- THE FIX: DYNAMIC Y-AXIS LABELS ---
            for (i in 0..4) {
                val y = height - (i * height / 4)
                drawLine(Color.LightGray, Offset(0f, y), Offset(width, y), strokeWidth = 1f)

                // If invertY is true, calculate the label values backwards so they track the flipped line!
                val progression = if (invertY) (4 - i) / 4f else i / 4f
                val labelVal = minVal + (range * progression)

                drawText(textMeasurer, "${labelVal.toInt()}$suffix", topLeft = Offset(0f, y - 16.dp.toPx()), style = TextStyle(Color.Gray, 10.sp))
            }

            val path = androidx.compose.ui.graphics.Path()
            val points = data.mapIndexed { index, value ->
                val normalizedY = (value - minVal) / range
                // Flipped points calculation matches the updated label logic above
                val actualY = if (invertY) normalizedY * height else height - (normalizedY * height)
                Offset(index * xStep, actualY)
            }

            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) { path.lineTo(points[i].x, points[i].y) }

            drawPath(path, color = lineColor, style = Stroke(width = 6f, join = androidx.compose.ui.graphics.StrokeJoin.Round))

            points.forEach { point ->
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
            }
        }
    }
}