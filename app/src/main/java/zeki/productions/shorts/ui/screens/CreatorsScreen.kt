package zeki.productions.shorts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.SearchResultItem

data class CreatorStats(
    val accountName: String,
    val totalViews: Int,
    val videos: List<VideoEntity>
)

@Composable
fun CreatorsScreen(
    allVideos: List<VideoEntity>,
    onAccountSelected: (String) -> Unit,
    onVideoSelected: (String, String) -> Unit // accountName, videoId
) {
    // Group, calculate stats, and sort by most viewed
    val roster = remember(allVideos) {
        allVideos.groupBy { it.accountName }
            .map { (name, videos) ->
                CreatorStats(
                    accountName = name,
                    totalViews = videos.sumOf { it.viewedCount },
                    videos = videos.sortedByDescending { it.viewedCount } // Show best videos first
                )
            }
            .sortedByDescending { it.totalViews }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        // Sticky Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.9f))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "THE ROSTER",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(roster, key = { it.accountName }) { creator ->
                CreatorCard(
                    creator = creator,
                    onAccountSelected = { onAccountSelected(creator.accountName) },
                    onVideoSelected = { videoId -> onVideoSelected(creator.accountName, videoId) }
                )
            }
        }
    }
}

@Composable
private fun CreatorCard(
    creator: CreatorStats,
    onAccountSelected: () -> Unit,
    onVideoSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0000)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF8B0000).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {

            // Header: Avatar & Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAccountSelected() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF8B0000), Color(0xFF4A0000)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = creator.accountName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Stats
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${creator.accountName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${creator.videos.size} Videos • ${creator.totalViews} Views",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // View Profile Arrow
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Profile",
                    tint = Color(0xFF8B0000)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Video Preview Strip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show up to 10 videos as a preview to save memory
                items(creator.videos.take(10), key = { it.id }) { video ->
                    Box(
                        modifier = Modifier
                            .width(100.dp) // Fixed width for horizontal scrolling
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        SearchResultItem(
                            video = video,
                            onClick = { onVideoSelected(video.id) }
                        )
                    }
                }
            }
        }
    }
}