package zeki.productions.shorts.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.logic.DecryptedImageLoader
import java.io.File

@Composable
fun SearchResultItem(
    video: VideoEntity,
    onClick: () -> Unit
) {
    // FIX: Cache ImageBitmap directly to prevent allocation thrashing on recomposition
    var thumbnail by remember(video.id) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(video.id) { mutableStateOf(true) }
    var hasError by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        isLoading = true
        val result = DecryptedImageLoader.load(File(video.imagePath))
        thumbnail = result?.asImageBitmap()
        hasError = (result == null)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .padding(2.dp)
            .background(Color(0xFF0A0000))
            .clickable { onClick() }
            .shimmer(isLoading)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )
        } else if (!isLoading && hasError) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFF4A0000), modifier = Modifier.size(24.dp))
                Text("ERR", color = Color(0xFF4A0000), style = MaterialTheme.typography.labelSmall)
            }
        }

        Text(
            text = "@${video.accountName}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            maxLines = 1
        )
    }
}

fun Modifier.shimmer(enabled: Boolean): Modifier = if (enabled) this.then(shimmer()) else this