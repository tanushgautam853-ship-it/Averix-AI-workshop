package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun AmbientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val particles = remember { mutableStateListOf<Particle>() }
    
    val colors = listOf(
        Color(0xFF8B5CF6).copy(alpha = 0.25f), // Purple
        Color(0xFF3B82F6).copy(alpha = 0.25f), // Blue
        Color(0xFF10B981).copy(alpha = 0.20f), // Green
        Color(0xFFEF4444).copy(alpha = 0.15f)  // Red
    )

    LaunchedEffect(size) {
        if (size.width > 0 && size.height > 0 && particles.isEmpty()) {
            for (i in 0 until 8) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * size.width,
                        y = Random.nextFloat() * size.height,
                        vx = (Random.nextFloat() - 0.5f) * 1.5f,
                        vy = (Random.nextFloat() - 0.5f) * 1.5f,
                        radius = Random.nextFloat() * 400f + 250f,
                        color = colors.random()
                    )
                )
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "time"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time // read to invalidate and force continuous redraw
            if (size.width == 0) return@Canvas
            
            for (p in particles) {
                p.x += p.vx
                p.y += p.vy
                
                if (p.x < -p.radius * 2) p.x = size.width + p.radius * 2
                if (p.x > size.width + p.radius * 2) p.x = -p.radius * 2
                if (p.y < -p.radius * 2) p.y = size.height + p.radius * 2
                if (p.y > size.height + p.radius * 2) p.y = -p.radius * 2

                val brush = Brush.radialGradient(
                    colors = listOf(p.color, Color.Transparent),
                    center = Offset(p.x, p.y),
                    radius = p.radius
                )
                drawCircle(brush = brush, radius = p.radius, center = Offset(p.x, p.y))
            }
        }
        content()
    }
}
