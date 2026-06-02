package zeki.productions.shorts.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.logic.DecryptedImageLoader
import java.io.File

@Composable
fun AdPlayer(
    ad: VideoEntity,
    exoPlayer: ExoPlayer?,
    isActive: Boolean,
    onSkip: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(isActive) {
        if (isActive) {
            timeLeft = 5
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer?.playWhenReady = isActive
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        if (ad.adType == "image") {
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(ad.id) {
                val bitmap = DecryptedImageLoader.load(File(ad.imagePath))
                imageBitmap = bitmap?.asImageBitmap()
            }
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Advertisement",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (ad.adType == "video" && exoPlayer != null) {
            AdTexturePlayerView(exoPlayer, Modifier.fillMaxSize())
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(16.dp)
                .padding(bottom = 100.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEAB308))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "Sponsored",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (timeLeft > 0) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                .clickable(enabled = timeLeft == 0) { onSkip() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (timeLeft > 0) {
                Text("Skip in $timeLeft", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("Skip Ad ⏭", color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AdTexturePlayerView(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
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
                    exoPlayer.setVideoTextureView(this)
                    addOnLayoutChangeListener { view, l, t, r, b, _, _, _, _ ->
                        applyFitMatrix(
                            view as android.view.TextureView,
                            r - l,
                            b - t,
                            videoSize.first,
                            videoSize.second
                        )
                    }
                }
            },
            modifier = modifier.clipToBounds(),
            update = { textureView ->
                applyFitMatrix(
                    textureView,
                    textureView.width,
                    textureView.height,
                    videoSize.first,
                    videoSize.second
                )
            },
            onRelease = { exoPlayer.clearVideoTextureView(it) }
        )
    }
}

private fun applyFitMatrix(
    textureView: android.view.TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    if (viewWidth == 0 || viewHeight == 0 || videoWidth == 0 || videoHeight == 0) return
    val scaleX = viewWidth.toFloat() / videoWidth
    val scaleY = viewHeight.toFloat() / videoHeight
    val minScale = minOf(scaleX, scaleY)
    val matrix = android.graphics.Matrix()
    matrix.setScale(minScale / scaleX, minScale / scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}