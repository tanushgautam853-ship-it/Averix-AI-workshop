package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun GlobalLoadingOverlay(
    isLoading: Boolean
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    // Consume all pointer events to prevent UI interaction
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            ParticleLoadingBackground()
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun ParticleLoadingBackground() {
    val particles = remember { List(30) { createRandomLoadingParticle() } }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(16)
            time += 0.016f
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val x = (p.baseX * w + Math.sin((time * p.speedX + p.offsetX).toDouble()) * 50f * w/1000f).toFloat()
            val y = (p.baseY * h + Math.cos((time * p.speedY + p.offsetY).toDouble()) * 50f * h/1000f).toFloat()
            
            val radius = p.size * (1f + Math.sin((time * p.pulseSpeed).toDouble()).toFloat() * 0.2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = 0.6f),
                        p.color.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = radius * 3f
                ),
                radius = radius * 3f,
                center = Offset(x, y),
                blendMode = BlendMode.Screen
            )
            
            drawCircle(
                color = p.color.copy(alpha = 0.8f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

private data class LoadingParticle(
    val baseX: Float,
    val baseY: Float,
    val speedX: Float,
    val speedY: Float,
    val offsetX: Float,
    val offsetY: Float,
    val pulseSpeed: Float,
    val size: Float,
    val color: Color
)

private fun createRandomLoadingParticle(): LoadingParticle {
    val colors = listOf(Color(0xFFFF3B30), Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFAF52DE))
    return LoadingParticle(
        baseX = Random.nextFloat(),
        baseY = Random.nextFloat(),
        speedX = Random.nextFloat() * 1.5f + 0.5f,
        speedY = Random.nextFloat() * 1.5f + 0.5f,
        offsetX = Random.nextFloat() * 10f,
        offsetY = Random.nextFloat() * 10f,
        pulseSpeed = Random.nextFloat() * 2f + 1f,
        size = Random.nextFloat() * 10f + 5f,
        color = colors.random()
    )
}
