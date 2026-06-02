package zeki.productions.shorts.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.logic.ExoPlayerPool
import zeki.productions.shorts.ui.components.VoidState
import zeki.productions.shorts.ui.screens.AdPlayer
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    videos: List<VideoEntity>,
    adInventory: List<VideoEntity>, // NEW: Separate pool of ads
    initialVideoId: String? = null,
    onVideoSeen: (String) -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit,
    onAccountSelected: (String) -> Unit,
    onImmersiveChange: (Boolean) -> Unit
) {
    if (videos.isEmpty()) {
        VoidState()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerPool = remember { ExoPlayerPool(context) }
    val pagerState = rememberPagerState(pageCount = { videos.size })

    var isScrubbing by remember { mutableStateOf(false) }
    var isAdLocked by remember { mutableStateOf(false) }

    var isAppForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showEndToast by remember { mutableStateOf(false) }

    // --- ADVERTISEMENT COORDINATOR ENGINE ---
    // Tracks how many organic swipes have occurred
    var swipeCount by remember { mutableIntStateOf(0) }
    var nextAdTarget by remember { mutableIntStateOf(Random.nextInt(8, 11)) } // Random 8, 9, or 10

    // Holds the currently active ad (if any)
    var activeAdOverride by remember { mutableStateOf<VideoEntity?>(null) }

    // We need to know if they actually moved to a new page
    var previousPage by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != previousPage) {
            swipeCount++
            previousPage = pagerState.currentPage

            // If we hit the magic number and we actually have ads in the DB
            if (swipeCount >= nextAdTarget && adInventory.isNotEmpty()) {
                activeAdOverride = adInventory.random() // Pick a random ad
                swipeCount = 0 // Reset counter
                nextAdTarget = Random.nextInt(8, 11) // Generate new target (8 to 10)
            } else {
                activeAdOverride = null
            }
        }
    }

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
            onImmersiveChange(false)
        }
    }

    LaunchedEffect(initialVideoId, videos) {
        if (!initialVideoId.isNullOrBlank()) {
            val index = videos.indexOfFirst { it.id == initialVideoId }
            if (index != -1) pagerState.scrollToPage(index)
        }
    }

    val activeVideoId = if (videos.isNotEmpty() && pagerState.currentPage < videos.size) {
        videos[pagerState.currentPage].id
    } else null

    LaunchedEffect(activeVideoId, isAppForeground, activeAdOverride) {
        if (activeVideoId == null) return@LaunchedEffect

        val currentIndex = pagerState.currentPage
        val window = mutableListOf<VideoEntity>()

        // Inject the active Ad into the ExoPlayer window so it pre-loads!
        if (activeAdOverride != null) {
            window.add(activeAdOverride!!)
        } else {
            window.add(videos[currentIndex])
        }

        if (currentIndex > 0) window.add(videos[currentIndex - 1])
        if (currentIndex < videos.size - 1) window.add(videos[currentIndex + 1])

        val targetId = activeAdOverride?.id ?: activeVideoId
        playerPool.updateWindow(window, targetId, isAppForeground)

        if (isAppForeground && activeAdOverride == null) {
            onVideoSeen(activeVideoId)
        }
    }

    val overscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0f && pagerState.currentPage == videos.size - 1) {
                    if (!showEndToast) {
                        showEndToast = true
                        scope.launch {
                            delay(2500)
                            showEndToast = false
                        }
                    }
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .nestedScroll(overscrollConnection)
    ) {

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> if (index < videos.size) videos[index].id else index },
            userScrollEnabled = !isScrubbing && !isAdLocked
        ) { page ->
            Box(modifier = Modifier
                .fillMaxSize()
                .clipToBounds()) {
                if (page < videos.size) {

                    // FIX: If we are currently on the page that triggered the Ad Override, show the Ad!
                    val isCurrentPageAndAdOverride =
                        (page == pagerState.currentPage && activeAdOverride != null)
                    val displayVideo =
                        if (isCurrentPageAndAdOverride) activeAdOverride!! else videos[page]

                    val player = playerPool.activePlayers[displayVideo.id]

                    if (displayVideo.isAd) {
                        LaunchedEffect(Unit) { onImmersiveChange(true) }

                        AdPlayer(
                            ad = displayVideo,
                            exoPlayer = player,
                            isActive = pagerState.currentPage == page && isAppForeground,
                            onLockChange = { isAdLocked = it },
                            onSkip = {
                                // Instantly dismiss the ad override so the underlying video appears
                                activeAdOverride = null
                            }
                        )
                    } else {
                        if (player != null) {
                            ShortVideoPlayer(
                                video = displayVideo,
                                exoPlayer = player,
                                isActive = pagerState.currentPage == page && isAppForeground,
                                onToggleFavorite = onToggleFavorite,
                                onScrubbingStateChanged = { isScrubbing = it },
                                onAccountSelected = onAccountSelected,
                                onImmersiveChange = onImmersiveChange
                            )
                        } else {
                            Box(Modifier
                                .fillMaxSize()
                                .background(Color.Black))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showEndToast,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(500)) + slideOutVertically(tween(500)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.95f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    "You've caught up! No more videos.",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}