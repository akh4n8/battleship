package com.ak.battleship.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.*

@Composable
actual fun VictoryConfettiOverlay() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("confetti.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )
    if (progress < 1f) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
