package zeki.productions.shorts.ui

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zeki.productions.shorts.data.VideoEntity
import kotlin.math.roundToInt

data class TapEvent(val offset: Offset, val time: Long)

@OptIn(UnstableApi::class)
@Composable
fun ShortVideoPlayer(
    video: VideoEntity,
    exoPlayer: ExoPlayer,
    isActive: Boolean,
    onToggleFavorite: (VideoEntity) -> Unit,
    onScrubbingStateChanged: (Boolean) -> Unit
) {
    var isPausedByUser by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }

    // Fixed Double Tap State
    var lastDoubleTap by remember { mutableStateOf<TapEvent?>(null) }

    LaunchedEffect(isScrubbing) { onScrubbingStateChanged(isScrubbing) }

    LaunchedEffect(isActive, isScrubbing) {
        while (isActive && !isScrubbing) {
            if (exoPlayer.duration > 0) {
                progress =
                    (exoPlayer.currentPosition.toFloat() / exoPlayer.duration).coerceIn(0f, 1f)
            }
            delay(200)
        }
    }

    LaunchedEffect(isActive, isPausedByUser) {
        exoPlayer.playWhenReady = isActive && !isPausedByUser
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
    ) {
        TexturePlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = { isPausedByUser = !isPausedByUser },
                    onDoubleTap = { offset ->
                        lastDoubleTap = TapEvent(offset, System.currentTimeMillis())
                        onToggleFavorite(video)
                    }
                )
            }
            .pointerInput(video.id) {
                detectHorizontalDragGestures(
                    onDragStart = { isScrubbing = true },
                    onDragEnd = {
                        exoPlayer.seekTo((progress * exoPlayer.duration).toLong())
                        isScrubbing = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 1.0f
                        progress = (progress + (dragAmount / size.width) * sensitivity).coerceIn(
                            0f,
                            1f
                        )
                    }
                )
            }
        )

        VideoInteractionOverlay(video, onToggleFavorite)

        // Play/Pause Indicator
        AnimatedVisibility(
            visible = isPausedByUser && !isScrubbing,
            enter = scaleIn(initialScale = 1.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Fixed Bouncy Heart Animation
        lastDoubleTap?.let { tap ->
            key(tap.time) { // Forces a complete restart of the animation on every new tap
                val alpha = remember { Animatable(1f) }
                val scale = remember { Animatable(0f) }
                val offsetY = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    scale.animateTo(1.3f, tween(150, easing = FastOutSlowInEasing))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                    delay(300)
                    launch { offsetY.animateTo(-250f, tween(600, easing = FastOutLinearInEasing)) }
                    alpha.animateTo(0f, tween(600))
                }

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFF8B0000).copy(alpha = alpha.value),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = tap.offset.x.roundToInt() - 130, // Center X
                                y = tap.offset.y.roundToInt() - 130 + offsetY.value.roundToInt() // Center Y + Float Up
                            )
                        }
                        .size(100.dp)
                        .scale(scale.value)
                        .shadow(12.dp, CircleShape, spotColor = Color.Red)
                )
            }
        }

        // Fixed Scrubber Timeline (Pushed up into visible area)
        val barHeight by animateDpAsState(if (isScrubbing) 8.dp else 2.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 90.dp) // Clears the Bottom Navigation Bar
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Color(0xFF8B0000))
            )
        }

        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp) // Floats above the timeline
        ) {
            Text(
                text = formatTime((progress * exoPlayer.duration).toLong()),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.shadow(4.dp)
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

        onDispose { exoPlayer.removeListener(listener) }
    }

    key(exoPlayer) {
        AndroidView(
            factory = { context ->
                android.view.TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    clipToOutline = true
                    outlineProvider = android.view.ViewOutlineProvider.BOUNDS
                    exoPlayer.setVideoTextureView(this)

                    addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
                        val width = right - left
                        val height = bottom - top
                        val (vw, vh) = videoSize
                        applyCenterCropMatrix(
                            view as android.view.TextureView,
                            width,
                            height,
                            vw,
                            vh
                        )
                    }
                }
            },
            modifier = modifier.clipToBounds(),
            update = { textureView ->
                val (vw, vh) = videoSize
                applyCenterCropMatrix(textureView, textureView.width, textureView.height, vw, vh)
            },
            onRelease = { textureView ->
                exoPlayer.clearVideoTextureView(textureView)
            }
        )
    }
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
    if (ms < 0) return "00:00"
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%02d:%02d".format(mins, secs)
}