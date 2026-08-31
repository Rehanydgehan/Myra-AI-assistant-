package com.myra.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.myra.assistant.ui.theme.Background
import com.myra.assistant.ui.theme.PrimaryRed
import com.myra.assistant.ui.theme.Purple
import com.myra.assistant.ui.theme.Success

@Composable
fun OrbAnimationView(isActive: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width / 2f * 0.8f // 80% of the box size
            val currentRadius = baseRadius * pulse
            
            // Outer Glow (Radial Gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryRed.copy(alpha = if (isActive) 0.3f else 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.5f
                ),
                radius = currentRadius * 1.5f,
                center = center
            )
            
            // Outer Ring 1
            drawCircle(
                color = PrimaryRed.copy(alpha = 0.2f),
                radius = currentRadius * 1.1f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Outer Ring 2 (Rotated)
            rotate(degrees = 45f + (if (isActive) rotation else 0f), pivot = center) {
                drawCircle(
                    color = Purple.copy(alpha = 0.1f),
                    radius = currentRadius * 1.25f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Dynamic expanding ripples when active
            if (isActive) {
                // Ripple 1
                val rippleRadius1 = currentRadius * (1f + rippleProgress * 0.8f)
                val rippleAlpha1 = (1f - rippleProgress) * 0.4f
                drawCircle(
                    color = PrimaryRed.copy(alpha = rippleAlpha1),
                    radius = rippleRadius1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Ripple 2 (offset)
                val rippleProgress2 = (rippleProgress + 0.5f) % 1f
                val rippleRadius2 = currentRadius * (1f + rippleProgress2 * 0.8f)
                val rippleAlpha2 = (1f - rippleProgress2) * 0.4f
                drawCircle(
                    color = Purple.copy(alpha = rippleAlpha2),
                    radius = rippleRadius2,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Inner core border gradient
            val innerCoreRadius = currentRadius * 0.75f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryRed, Purple, PrimaryRed)
                ),
                radius = innerCoreRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Dark center
            drawCircle(
                color = Background,
                radius = innerCoreRadius - 2.dp.toPx(),
                center = center
            )
            
            // Gradient Overlay inside center
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, PrimaryRed.copy(alpha = 0.2f), Color.Transparent),
                    startY = center.y - innerCoreRadius,
                    endY = center.y + innerCoreRadius
                ),
                radius = innerCoreRadius - 2.dp.toPx(),
                center = center
            )
            
            // Center bright spot (Radial Gradient instead of blur)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isActive) Purple.copy(alpha = 0.6f) else PrimaryRed.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerCoreRadius * 0.6f
                ),
                radius = innerCoreRadius * 0.6f,
                center = center
            )
        }
        
        // Status dot
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Success)
        )
    }
}
