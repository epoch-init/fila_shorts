package zeki.productions.shorts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.SearchResultItem

@Composable
fun ProfileScreen(
    accountName: String,
    allVideos: List<VideoEntity>,
    onBack: () -> Unit,
    onVideoSelected: (String) -> Unit
) {
    val accountVideos = remember(allVideos, accountName) {
        allVideos.filter { it.accountName.equals(accountName, ignoreCase = true) }
    }

    val totalViews = remember(accountVideos) { accountVideos.sumOf { it.viewedCount } }
    val totalLikes = remember(accountVideos) { accountVideos.count { it.isFavorite } }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // App Bar
        Box(modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = accountName.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Profile Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8B0000), Color(0xFF4A0000))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = accountName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("@$accountName", color = Color.White, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Videos", accountVideos.size.toString())
                StatColumn("Likes", totalLikes.toString())
                StatColumn("Views", totalViews.toString())
            }
        }

        Divider(color = Color.DarkGray, thickness = 1.dp)

        // Video Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp)
        ) {
            items(accountVideos) { video ->
                SearchResultItem(video = video, onClick = { onVideoSelected(video.id) })
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
    }
}