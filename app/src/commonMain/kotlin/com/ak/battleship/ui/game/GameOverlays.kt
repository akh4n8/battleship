package com.ak.battleship.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 1. DNA container for our tumbling ticker tape
data class TickerTape(
    val startX: Float,
    val startY: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val spinSpeed: Float,
    val spinPhase: Float,
    val driftPhase: Float,
    val color: Color
)

@Composable
fun FiniteVictoryOverlay(onAnimationFinished: () -> Unit) {
    // Timer to control the lifecycle (4 seconds)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5700, easing = LinearEasing)
        )
        onAnimationFinished() // Cleanly self-destruct
    }

    // Fade out smoothly in the very last 10% of the animation
    val globalAlpha = when {
        progress.value > 0.9f -> (1f - progress.value) / 0.1f // Pushed from 0.8f to 0.9f
        else -> 1f
    }

    // Generate 120 pieces of elegant, thematic confetti
    val particles = remember {
        List(120) {
            TickerTape(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * -2f,
                width = Random.nextFloat() * 18f + 12f,
                height = Random.nextFloat() * 30f + 15f,

                // Cut the fall speed in half so it floats gracefully
                speed = Random.nextFloat() * 0.6f + 0.2f,

                // Slowed down the 3D flipping so it's less frantic
                spinSpeed = Random.nextFloat() * 4f + 2f,

                spinPhase = Random.nextFloat() * 100f,
                driftPhase = Random.nextFloat() * 100f,
                color = listOf(
                    Color(0xFFD32F2F), // Crimson Red (Matches the "HIT" text and grid crosses)
                    Color(0xFF1976D2), // Deep Blue (Matches the "SUNK" text and ship outlines)
                    Color(0xFFFFB63D), // Solid Gold (A slightly more saturated version of your "WIN" badge)
                    Color(0xFF5E35B1), // Deep Purple (Matches the "Offense / Defense" tab headers)
                    Color(0xFF4CAF50)  // Green (Matches the "Defense" dot indicators in your Move History)
                ).random()
            )
        }
    }

    // Custom Game Loop for the physics
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (progress.value < 1f) {
            val frameTime = withFrameNanos { it }
            val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
            lastFrameTime = frameTime
            time += deltaMs / 1000f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                // Calculate the downward drop based on time
                val currentY = (p.startY * h) + (time * p.speed * h)

                // Add a gentle side-to-side sway as it falls
                val drift = sin(time * 2f + p.driftPhase) * (w * 0.08f)
                val currentX = (p.startX * w) + drift

                // The 3D Trick: We scale the height using a cosine wave.
                // As cos() goes from 1 to 0, the rectangle flattens, making it look like it's flipping.
                val apparentHeight = p.height * abs(cos(time * p.spinSpeed + p.spinPhase))

                // Only draw if it is currently visible on the screen
                if (currentY > -50f && currentY < h + 50f) {
                    drawRect(
                        color = p.color.copy(alpha = globalAlpha),
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.width, apparentHeight.toFloat())
                    )
                }
            }
        }
    }
}

// 1. Simple DNA container for our falling particles
data class AshParticle(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val driftPhase: Float,
    val color: Color
)

@Composable
fun FiniteDefeatOverlay(onAnimationFinished: () -> Unit) {
    // A single timer that runs from 0.0 to 1.0 over 4 seconds
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
        )
        onAnimationFinished() // Self-destruct when finished
    }

    // Math to fade in quickly (first 10%) and fade out smoothly (last 20%)
    val globalAlpha = when {
        progress.value < 0.1f -> progress.value / 0.1f
        progress.value > 0.8f -> (1f - progress.value) / 0.2f
        else -> 1f
    }

    // Generate 100 random ash/ember particles once
    val particles = remember {
        List(100) {
            AshParticle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * 1.5f - 0.5f, // Start slightly higher off screen
                size = Random.nextFloat() * 6f + 3f, // Slightly larger to be seen over the UI
                speed = Random.nextFloat() * 1.5f + 0.5f, // Speed multiplier
                driftPhase = Random.nextFloat() * 100f,
                color = listOf(
                    Color(0xFF8B0000), // Dark Red
                    Color(0xFF212121), // Charcoal
                    Color(0xFFD84315)  // Deep Ember Orange
                ).random()
            )
        }
    }

    // Box no longer has the dark gradient background!
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                // Use the animation progress to drive the Y distance
                val currentY = (p.startY * h) + (progress.value * p.speed * h)
                val drift = sin(progress.value * 10f + p.driftPhase) * (w * 0.05f)
                val currentX = (p.startX * w) + drift

                // Only draw if it hasn't fallen off the bottom of the screen
                if (currentY < h + 50f) {
                    drawCircle(
                        color = p.color.copy(alpha = globalAlpha * 0.9f),
                        radius = p.size,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}