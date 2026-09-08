package com.ak.battleship.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.HeatmapData
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapsTab(
    data: HeatmapData?,
    selectedSubTab: Int,
    onSubTabChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            edgePadding = 8.dp
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { onSubTabChange(0) }) { Text("My Ships", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = selectedSubTab == 1, onClick = { onSubTabChange(1) }) { Text("Opp. Ships", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = selectedSubTab == 2, onClick = { onSubTabChange(2) }) { Text("My Attacks", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = selectedSubTab == 3, onClick = { onSubTabChange(3) }) { Text("Opp. Attacks", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = selectedSubTab == 4, onClick = { onSubTabChange(4) }) { Text("My Efficiency", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = selectedSubTab == 5, onClick = { onSubTabChange(5) }) { Text("Opp. Efficiency", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (data == null) {
            SonarLoader("GENERATING THERMAL MAPS...")
        } else {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                when (selectedSubTab) {
                    0 -> HeatmapGrid(data.userShips, data.maxUserShips, Color(0xFF388E3C))
                    1 -> HeatmapGrid(data.oppShips, data.maxOppShips, Color(0xFF388E3C))
                    2 -> HeatmapGrid(data.userAttacks, data.maxUserAttacks, Color(0xFFD32F2F))
                    3 -> HeatmapGrid(data.oppAttacks, data.maxOppAttacks, Color(0xFFD32F2F))
                    4 -> HeatmapGrid(dataMap = data.userEfficiency, data.maxUserEfficiency, baseColor = Color(0xFF388E3C), isPercentage = true)
                    5 -> HeatmapGrid(dataMap = data.oppEfficiency, data.maxOppEfficiency, baseColor = Color(0xFFD32F2F), isPercentage = true)
                }
            }

            val subtitleText = when (selectedSubTab) {
                in 0..3 -> "Darker color = higher frequency.\nNumbers indicate exact count."
                else -> "Darker color = higher combat precision.\nNumbers indicate exact hit percentage (hits/total shots)."
            }

            Text(subtitleText, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

@Composable
fun HeatmapGrid(dataMap: Map<Pair<Int, Int>, Int>, maxCount: Int, baseColor: Color, isPercentage: Boolean = false) {
    val textMeasurer = rememberTextMeasurer()

    val colLabels = remember { (1..10).map { textMeasurer.measure(it.toString(), TextStyle(Color.Black, 14.sp, FontWeight.Bold)) } }
    val rowLabels = remember { (0..9).map { textMeasurer.measure((it + 65).toChar().toString(), TextStyle(Color.Black, 14.sp, FontWeight.Bold)) } }

    val cachedCellTextLayouts = remember(dataMap, maxCount, baseColor, isPercentage) {
        dataMap.filter { it.value > 0 }.mapValues { (_, count) ->
            val alpha = if (maxCount == 0) 0f else count.toFloat() / maxCount.toFloat()
            val textColor = if (alpha > 0.6f) Color.White else Color.Black

            // THE FIX: Using the explicit boolean flag instead of guessing based on maxCount
            val labelText = if (isPercentage) "$count%" else count.toString()

            textMeasurer.measure(labelText, style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        val cellSize = size.width / 11f

        colLabels.forEachIndexed { i, layout -> drawText(layout, topLeft = Offset(cellSize + (i * cellSize) + (cellSize * 0.3f), cellSize * 0.2f)) }
        rowLabels.forEachIndexed { i, layout -> drawText(layout, topLeft = Offset(cellSize * 0.2f, cellSize + (i * cellSize) + (cellSize * 0.2f))) }

        for (col in 0..9) {
            for (row in 0..9) {
                val cellKey = Pair(col, row)
                val count = dataMap[cellKey] ?: 0
                val alpha = if (maxCount == 0) 0f else count.toFloat() / maxCount.toFloat()
                val topLeft = Offset(cellSize + col * cellSize, cellSize + row * cellSize)

                drawRect(baseColor.copy(alpha = alpha.coerceIn(0f, 1f)), topLeft, Size(cellSize, cellSize))
                drawRect(Color.Black.copy(alpha = 0.2f), topLeft, Size(cellSize, cellSize), style = Stroke(1f))

                cachedCellTextLayouts[cellKey]?.let { textLayout ->
                    drawText(textLayout, topLeft = Offset(topLeft.x + (cellSize / 2f) - (textLayout.size.width / 2f), topLeft.y + (cellSize / 2f) - (textLayout.size.height / 2f)))
                }
            }
        }
    }
}