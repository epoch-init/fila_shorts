package zeki.productions.shorts.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SwipeTutorialOverlay(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_anim")

    // Animate the arrow moving upwards and fading out
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // Dim the video behind it
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (dragAmount.y < -10) onDismiss() // Dismiss on swipe up
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe Up",
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = offsetY.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Swipe up for next video",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}