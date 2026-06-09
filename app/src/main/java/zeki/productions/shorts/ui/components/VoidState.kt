package zeki.productions.shorts.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VoidState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            RadarScanner()
            Spacer(modifier = Modifier.height(40.dp))
            TypewriterText("Scanning...\nNo .short files detected in\n /FILA TikTok.")
        }
    }
}

@Composable
private fun RadarScanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = primaryColor.copy(alpha = 0.2f), style = Stroke(width = 2f))
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = size.minDimension / 3,
                style = Stroke(width = 1f)
            )

            rotate(rotation) {
                val sweep = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1.0f to primaryColor
                )
                drawCircle(brush = sweep)
            }
        }
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = primaryColor)
        }
    }
}

@Composable
private fun TypewriterText(fullText: String) {
    var textToDisplay by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        textToDisplay = ""
        fullText.forEachIndexed { index, _ ->
            textToDisplay = fullText.substring(0, index + 1)
            delay(40)
        }
    }

    Text(
        text = textToDisplay,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center
    )
}