package zeki.productions.shorts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.SearchResultItem

@Composable
fun SearchScreen(
    videos: List<VideoEntity>,
    onVideoSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filteredVideos = remember(query, videos) {
        if (query.isBlank()) emptyList()
        else videos.filter {
            it.description.contains(query, ignoreCase = true) ||
                    it.accountName.contains(query, ignoreCase = true)
        }
    }

    val accounts = remember(query, videos) {
        if (query.isBlank()) emptyList()
        else videos.filter { it.accountName.contains(query, ignoreCase = true) }
            .groupBy { it.accountName }
            .map { it.key to it.value.size }
    }

    val hashtags = remember(query, videos) {
        if (query.isBlank()) emptyList()
        else videos.flatMap { it.categories.split(",") }
            .filter { it.contains(query, ignoreCase = true) && it.isNotBlank() }
            .groupBy { it }
            .map { it.key to it.value.size }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search accounts, tags, or content...") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF1A0000),
                focusedContainerColor = Color(0xFF2A0000),
                cursorColor = Color.Red,
                focusedIndicatorColor = Color.Red
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            if (accounts.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Accounts") }
                items(accounts, span = { GridItemSpan(maxLineSpan) }) { (name, count) ->
                    AccountListItem(name, count)
                }
            }

            if (hashtags.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Hashtags") }
                items(hashtags, span = { GridItemSpan(maxLineSpan) }) { (tag, count) ->
                    HashtagListItem(tag, count)
                }
            }

            if (filteredVideos.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Video Results") }
                items(filteredVideos) { video ->
                    SearchResultItem(video = video, onClick = { onVideoSelected(video.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp),
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun AccountListItem(name: String, count: Int) {
    ListItem(
        headlineContent = { Text("@$name", color = Color.White, fontWeight = FontWeight.Bold) },
        supportingContent = { Text("$count videos", color = Color.Gray) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun HashtagListItem(tag: String, count: Int) {
    ListItem(
        headlineContent = { Text("#$tag", color = Color.White) },
        supportingContent = { Text("$count videos", color = Color.Gray) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}