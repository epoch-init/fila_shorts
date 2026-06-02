package zeki.productions.shorts.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import zeki.productions.shorts.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logoScale = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-45f) }
    val textBlur = remember { Animatable(20f) }
    val textAlpha = remember { Animatable(0f) }
    val letterSpacing = remember { Animatable(20f) }

    // Dynamic Ambient Background
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradient"
    )

    // Staggered Shockwaves
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutLinearInEasing),
            RepeatMode.Restart
        ), label = "w1"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutLinearInEasing),
            RepeatMode.Restart
        ), label = "w1a"
    )

    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutLinearInEasing, delayMillis = 300),
            RepeatMode.Restart
        ), label = "w2"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutLinearInEasing, delayMillis = 300),
            RepeatMode.Restart
        ), label = "w2a"
    )

    LaunchedEffect(Unit) {
        // Dramatic Logo Drop
        logoScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        logoRotation.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))

        // Cinematic Text Un-blur and Tighten
        textAlpha.animateTo(1f, tween(600, easing = LinearEasing))
        textBlur.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
        letterSpacing.animateTo(2f, tween(1000, easing = FastOutSlowInEasing))

        delay(600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = gradientShift + 500f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Shockwave 1
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(wave1Scale)
                        .alpha(wave1Alpha)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                // Shockwave 2
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(wave2Scale)
                        .alpha(wave2Alpha)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                // Logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(logoScale.value)
                        .graphicsLayer { rotationZ = logoRotation.value }
                        .clip(RoundedCornerShape(24.dp))
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "App Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "FILA SPORTS",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing.value.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .blur(textBlur.value.dp)
            )
        }
    }
}