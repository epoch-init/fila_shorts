package zeki.productions.shorts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.SearchResultItem

@Composable
fun FavoritesListScreen(
    allVideos: List<VideoEntity>,
    onBack: () -> Unit,
    onVideoSelected: (String) -> Unit
) {
    val favoriteVideos = remember(allVideos) {
        allVideos.filter { it.isFavorite }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "LIKED VIDEOS",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (favoriteVideos.isEmpty()) {
            // Empty State
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Stats Header
            Text(
                text = "${favoriteVideos.size} saved clips",
                color = Color.Gray,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider(color = Color(0xFF1A1A1A), thickness = 1.dp)

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(favoriteVideos, key = { it.id }) { video ->
                    SearchResultItem(video = video, onClick = { onVideoSelected(video.id) })
                }
            }
        }
    }
}