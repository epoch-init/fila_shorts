package zeki.productions.shorts.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zeki.productions.shorts.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onSplashFinished: () -> Any) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }

    val shockwaveScale = remember { Animatable(0.5f) }
    val shockwaveAlpha = remember { Animatable(0f) }

    val badgeOffsetY = remember { Animatable(-20f) }
    val badgeAlpha = remember { Animatable(0f) }

    val bottomPanelOffsetY = remember { Animatable(100f) }
    val bottomPanelAlpha = remember { Animatable(0f) }

    val flag1Scale = remember { Animatable(0f) }
    val flag2Scale = remember { Animatable(0f) }
    val flag3Scale = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit1"
    )
    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit2"
    )

    LaunchedEffect(Unit) {
        delay(2500)
        launch { onSplashFinished() }
    }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(1000, easing = LinearEasing)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 50f)
            )
            launch { shockwaveAlpha.animateTo(0.6f, tween(400)) }
            launch {
                shockwaveScale.animateTo(
                    2.5f,
                    tween(1500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
                )
            }
            launch { shockwaveAlpha.animateTo(0f, tween(1000, delayMillis = 400)) }
        }

        delay(800)
        launch { badgeAlpha.animateTo(1f, tween(800)) }
        launch { badgeOffsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 100f)) }

        delay(600)
        launch { bottomPanelAlpha.animateTo(1f, tween(800)) }
        launch { bottomPanelOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 50f)) }

        delay(400)
        launch { flag1Scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
        delay(200)
        launch { flag2Scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
        delay(200)
        launch { flag3Scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0214)), // Deep WC Navy
        contentAlignment = Alignment.Center
    ) {
        // --- Ambient Spotlights ---
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = cos(Math.toRadians(orbitAngle1.toDouble())).toFloat() * 400f
                        translationY = sin(Math.toRadians(orbitAngle1.toDouble())).toFloat() * 400f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00FF87).copy(alpha = 0.15f),
                                Color.Transparent
                            ), // Neon Green
                            radius = 800f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = cos(Math.toRadians(orbitAngle2.toDouble())).toFloat() * -500f
                        translationY = sin(Math.toRadians(orbitAngle2.toDouble())).toFloat() * 300f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF0055).copy(alpha = 0.15f),
                                Color.Transparent
                            ), // Magenta
                            radius = 900f
                        )
                    )
            )
        }

        // --- Center Focus (Logo & Badge) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale.value
                scaleY = logoScale.value
                alpha = logoAlpha.value
                translationY = if (logoScale.value == 1f) floatY else 0f
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = shockwaveScale.value
                            scaleY = shockwaveScale.value
                            alpha = shockwaveAlpha.value
                        }
                        .border(2.dp, Color(0xFF00FF87), CircleShape)
                        .blur(8.dp)
                )

                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = "FILA World Cup Logo",
                    modifier = Modifier.width(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.graphicsLayer {
                    translationY = badgeOffsetY.value
                    alpha = badgeAlpha.value
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFFF0055), CircleShape) // Magenta Dot
                            .blur(1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FILA TikTok",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "THE CONTENT YOU LOVE CURATED FOR YOU",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    translationY = badgeOffsetY.value
                    alpha = badgeAlpha.value
                }
            )
        }

        // --- Bottom Host Nations Glass Panel ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .graphicsLayer {
                    translationY = bottomPanelOffsetY.value
                    alpha = bottomPanelAlpha.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "POWERED BY",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(R.drawable.nega_production_logo),
                    contentDescription = "Nega Production",
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HostNationLogo(drawableRes: Int, scaleAnim: Float) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                alpha = scaleAnim.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}