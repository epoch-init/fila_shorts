package zeki.productions.shorts.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition("shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    // Blood Red / Oxblood Shimmer
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A0000),
            Color(0xFF4A0000),
            Color(0xFF1A0000)
        ),
        start = Offset.Zero,
        end = Offset(translateAnim, translateAnim)
    )
    background(brush)
}