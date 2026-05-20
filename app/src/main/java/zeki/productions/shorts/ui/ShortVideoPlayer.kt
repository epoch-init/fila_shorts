package zeki.productions.shorts.ui

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.logic.HapticManager

@OptIn(UnstableApi::class)
@Composable
fun ShortVideoPlayer(
    video: VideoEntity,
    exoPlayer: ExoPlayer,
    isActive: Boolean,
    onToggleFavorite: (VideoEntity) -> Unit,
    onScrubbingStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isPausedByUser by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubTime by remember { mutableStateOf("00:00") }

    // Tracks the exact pixel coordinates of the user's double tap
    var heartTrigger by remember { mutableStateOf<Pair<Long, Offset>?>(null) }
    var lastHapticProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isScrubbing) { onScrubbingStateChanged(isScrubbing) }

    LaunchedEffect(isActive, isScrubbing) {
        while (isActive && !isScrubbing) {
            if (exoPlayer.duration > 0) {
                progress =
                    (exoPlayer.currentPosition.toFloat() / exoPlayer.duration).coerceIn(0f, 1f)
            }
            delay(100) // Lowered delay slightly for a smoother progress bar
        }
    }

    LaunchedEffect(isActive, isPausedByUser) {
        exoPlayer.playWhenReady = isActive && !isPausedByUser
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .clipToBounds()
        .background(Color.Black)
        .pointerInput(video.id) {
            detectHorizontalDragGestures(
                onDragStart = {
                    isScrubbing = true
                    lastHapticProgress = progress
                    HapticManager.thud(context)
                },
                onDragEnd = {
                    exoPlayer.seekTo((progress * exoPlayer.duration).toLong())
                    isScrubbing = false
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    val sensitivity = 1.0f
                    progress = (progress + (dragAmount / size.width) * sensitivity).coerceIn(0f, 1f)

                    val currentPos = (progress * exoPlayer.duration).toLong()
                    scrubTime = "${formatTime(currentPos)} / ${formatTime(exoPlayer.duration)}"

                    // Tick haptic feedback every 2% scrubbed
                    if (kotlin.math.abs(progress - lastHapticProgress) > 0.02f) {
                        HapticManager.lightTick(context)
                        lastHapticProgress = progress
                    }
                }
            )
        }
    ) {
        TexturePlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        isPausedByUser = !isPausedByUser
                        HapticManager.thud(context)
                    },
                    onDoubleTap = { offset ->
                        onToggleFavorite(video)
                        heartTrigger = System.currentTimeMillis() to offset
                        HapticManager.thud(context)
                    }
                )
            }
        )

        // Dim the screen and show Play icon dynamically when paused
        AnimatedVisibility(
            visible = isPausedByUser,
            enter = scaleIn(
                spring(
                    dampingRatio = 0.6f,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = scaleOut(tween(200)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(90.dp)
                )
            }
        }

        VideoInteractionOverlay(video, onToggleFavorite)

        // Bouncy Floating Heart Animation
        DoubleTapHeartAnimation(trigger = heartTrigger)

        // Magnetic Timeline / Scrubber
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (isScrubbing) 48.dp else 0.dp) // Lift when scrubbing
        ) {
            val barHeight by animateDpAsState(
                targetValue = if (isScrubbing) 6.dp else 2.dp,
                label = "barHeight"
            )
            val barAlpha by animateFloatAsState(
                targetValue = if (isScrubbing) 1f else 0.3f,
                label = "barAlpha"
            )

            // Timeline Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .background(Color.White.copy(alpha = 0.2f * barAlpha))
                    .align(Alignment.BottomStart)
            )

            // Timeline Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .height(barHeight)
                    .background(Color.White.copy(alpha = barAlpha))
                    .align(Alignment.BottomStart)
            )

            // Time Tooltip
            AnimatedVisibility(
                visible = isScrubbing,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-32).dp)
            ) {
                Text(
                    text = scrubTime,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.shadow(4.dp)
                )
            }
        }
    }
}

@Composable
fun DoubleTapHeartAnimation(trigger: Pair<Long, Offset>?) {
    trigger ?: return
    val density = LocalDensity.current

    // Key ensures the animation restarts completely on rapid double-taps
    key(trigger.first) {
        var isAnimating by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isAnimating = true
            delay(800) // Animation lifetime
            isAnimating = false
        }

        val scale by animateFloatAsState(
            targetValue = if (isAnimating) 1.2f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "heartScale"
        )

        val alpha by animateFloatAsState(
            targetValue = if (isAnimating) 1f else 0f,
            animationSpec = tween(durationMillis = if (isAnimating) 150 else 400),
            label = "heartAlpha"
        )

        val translationY by animateFloatAsState(
            targetValue = if (isAnimating) -200f else 0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "heartTransY"
        )

        val rotation by animateFloatAsState(
            targetValue = if (isAnimating) listOf(-15f, 0f, 15f).random() else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "heartRot"
        )

        if (alpha > 0f) {
            val iconSizeDp = 100.dp
            val iconSizePx = with(density) { iconSizeDp.toPx() }

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE50000), // Vibrant Red
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (trigger.second.x - (iconSizePx / 2)).toInt(),
                            y = (trigger.second.y - (iconSizePx / 2) + translationY).toInt()
                        )
                    }
                    .size(iconSizeDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        rotationZ = rotation
                        shadowElevation = 16f
                    }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TexturePlayerView(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    // UNTOUCHED: Optimized direct texture rendering engine
    var videoSize by remember { mutableStateOf(Pair(0, 0)) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    val scaledWidth = (size.width * size.pixelWidthHeightRatio).toInt()
                    videoSize = Pair(scaledWidth, size.height)
                }
            }
        }
        exoPlayer.addListener(listener)

        val currentSize = exoPlayer.videoSize
        if (currentSize.width > 0 && currentSize.height > 0) {
            val scaledWidth = (currentSize.width * currentSize.pixelWidthHeightRatio).toInt()
            videoSize = Pair(scaledWidth, currentSize.height)
        }

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    AndroidView(
        factory = { context ->
            android.view.TextureView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BOUNDS

                addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
                    val width = right - left
                    val height = bottom - top
                    val (vw, vh) = videoSize
                    applyCenterCropMatrix(view as android.view.TextureView, width, height, vw, vh)
                }
            }
        },
        modifier = modifier.clipToBounds(),
        update = { textureView ->
            exoPlayer.clearVideoTextureView(textureView)
            exoPlayer.setVideoTextureView(textureView)
            val (vw, vh) = videoSize
            applyCenterCropMatrix(textureView, textureView.width, textureView.height, vw, vh)
        }
    )
}

private fun applyCenterCropMatrix(
    textureView: android.view.TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    if (viewWidth == 0 || viewHeight == 0 || videoWidth == 0 || videoHeight == 0) return

    val scaleX = viewWidth.toFloat() / videoWidth
    val scaleY = viewHeight.toFloat() / videoHeight
    val maxScale = maxOf(scaleX, scaleY)

    val scaleCorrectionX = maxScale / scaleX
    val scaleCorrectionY = maxScale / scaleY

    val matrix = android.graphics.Matrix()
    matrix.setScale(scaleCorrectionX, scaleCorrectionY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%02d:%02d".format(mins, secs)
}