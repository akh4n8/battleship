package com.ak.battleship

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.ak.battleship.ui.AppNavigation
import com.ak.battleship.ui.theme.BattleshipTheme
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.data.DummyRepository

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        BattleshipTheme(darkTheme = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val isDesktop = maxWidth > 560.dp
                    val appModifier = if (isDesktop) {
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = 480.dp)
                            .padding(vertical = 12.dp)
                            .shadow(16.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Box(modifier = appModifier) {
                        val viewModel = remember { BattleshipViewModel(DummyRepository(), null) }
                        AppNavigation(viewModel)
                    }
                }
            }
        }
    }
}
