package zeki.productions.shorts.ui.screens

import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.SearchResultItem

@Composable
fun SearchScreen(
    videos: List<VideoEntity>,
    onVideoSelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filteredVideos = remember(query, videos) {
        if (query.isBlank()) {
            videos.shuffled()
        } else {
            videos.filter {
                it.description.contains(query, ignoreCase = true) ||
                        it.accountName.contains(query, ignoreCase = true) ||
                        it.categories.contains(query, ignoreCase = true) ||
                        it.id.contains(query, ignoreCase = true)
            }
        }
    }

    val accounts = remember(query, videos) {
        if (query.isBlank()) emptyList()
        else videos.filter { it.accountName.contains(query, ignoreCase = true) }
            .groupBy { it.accountName }
            .map { it.key to it.value.size }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // FIX: Replaced Compose TextField with Crash-Proof Native EditText wrapper
            SafeSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            if (accounts.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Accounts") }
                items(accounts) { (name, count) ->
                    AccountPillCard(name, count) { onAccountSelected(name) }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(if (query.isBlank()) "Explore" else "Videos")
            }
            items(filteredVideos, key = { it.id }) { video ->
                val aspect = remember(video.id) { listOf(0.8f, 1.2f, 1.5f).random() }
                Box(modifier = Modifier.aspectRatio(1f / aspect)) {
                    SearchResultItem(video = video, onClick = { onVideoSelected(video.id) })
                }
            }
        }
    }
}

/**
 * A perfectly stable Native Android EditText wrapped for Compose.
 * Bypasses the Compose Semantics/IME R8 bug entirely.
 */
@Composable
private fun SafeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = Color.Gray.toArgb()
    val cursorTint = MaterialTheme.colorScheme.primary.toArgb()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            factory = { context ->
                EditText(context).apply {
                    this.hint = "Search by filename, accounts, tags..."
                    this.setTextColor(textColor)
                    this.setHintTextColor(hintColor)
                    this.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.setPadding(0, 0, 0, 0)
                    this.textSize = 16f
                    this.maxLines = 1
                    this.inputType = InputType.TYPE_CLASS_TEXT
                    this.imeOptions = EditorInfo.IME_ACTION_SEARCH

                    // Safely apply theme cursor color for API 29+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        this.textCursorDrawable?.setTint(cursorTint)
                    }

                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable?) {
                            onQueryChange(s?.toString() ?: "")
                        }
                    })
                }
            },
            update = { view ->
                if (view.text.toString() != query) {
                    val selection = view.selectionStart
                    view.setText(query)
                    view.setSelection(selection.coerceAtMost(query.length))
                }
                // Ensure text color updates instantly if the user changes the app theme
                view.setTextColor(textColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    view.textCursorDrawable?.setTint(cursorTint)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 8.dp),
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun AccountPillCard(name: String, count: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "@$name",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$count videos",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}