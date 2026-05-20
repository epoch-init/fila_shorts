package zeki.productions.shorts.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.logic.ExoPlayerPool
import zeki.productions.shorts.ui.components.VoidState

/**
 * v1.9.4: Lifecycle-Synchronized Feed.
 * Listens to system ON_PAUSE/ON_RESUME events to lock/unlock ExoPlayer playback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    videos: List<VideoEntity>,
    initialVideoId: String? = null,
    onVideoSeen: (String) -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit
) {
    if (videos.isEmpty()) {
        VoidState()
        return
    }

    val context = LocalContext.current
    val playerPool = remember { ExoPlayerPool(context) }
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var isPagerLocked by remember { mutableStateOf(false) }

    // Lifecycle tracking to prevent audio bleeding in background
    var isAppForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isAppForeground = false
                    playerPool.pauseAll()
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppForeground = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerPool.release()
        }
    }

    LaunchedEffect(initialVideoId, videos) {
        if (!initialVideoId.isNullOrBlank()) {
            val index = videos.indexOfFirst { it.id == initialVideoId }
            if (index != -1) pagerState.scrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.currentPage, videos, isAppForeground) {
        val currentIndex = pagerState.currentPage
        if (currentIndex < 0 || currentIndex >= videos.size) return@LaunchedEffect

        val activeId = videos[currentIndex].id
        val window = mutableListOf<VideoEntity>()

        window.add(videos[currentIndex])
        if (currentIndex > 0) window.add(videos[currentIndex - 1])
        if (currentIndex < videos.size - 1) window.add(videos[currentIndex + 1])

        playerPool.updateWindow(window, activeId, isAppForeground)
        if (isAppForeground) onVideoSeen(activeId)
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black).clipToBounds(),
        key = { index -> if (index < videos.size) videos[index].id else index },
        userScrollEnabled = !isPagerLocked
    ) { page ->
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            if (page < videos.size) {
                val video = videos[page]
                val player = playerPool.activePlayers[video.id]

                if (player != null) {
                    ShortVideoPlayer(
                        video = video,
                        exoPlayer = player,
                        isActive = pagerState.currentPage == page && isAppForeground,
                        onToggleFavorite = onToggleFavorite,
                        onScrubbingStateChanged = { isPagerLocked = it }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }
            }
        }
    }
}