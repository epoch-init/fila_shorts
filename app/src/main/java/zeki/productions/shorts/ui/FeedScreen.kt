package zeki.productions.shorts.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
    adInventory: List<VideoEntity>,
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

    var isAppForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showEndToast by remember { mutableStateOf(false) }

    // --- OVERLAY AD COORDINATOR ---
    var swipeCount by remember { mutableIntStateOf(0) }
    var nextAdTarget by remember { mutableIntStateOf(Random.nextInt(8, 11)) }
    var activeAdOverride by remember { mutableStateOf<VideoEntity?>(null) }
    var previousPage by remember { mutableIntStateOf(pagerState.currentPage) }

    DisposableEffect(activeAdOverride) {
        onDispose {
            if (activeAdOverride == null) onImmersiveChange(false)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != previousPage) {
            swipeCount++
            previousPage = pagerState.currentPage

            if (swipeCount >= nextAdTarget && adInventory.isNotEmpty()) {
                activeAdOverride = adInventory.random()
                swipeCount = 0
                nextAdTarget = Random.nextInt(8, 11)
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

        window.add(videos[currentIndex])
        if (currentIndex > 0) window.add(videos[currentIndex - 1])
        if (currentIndex < videos.size - 1) window.add(videos[currentIndex + 1])

        if (activeAdOverride != null) {
            window.add(activeAdOverride!!)
        }

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

        // --- BASE LAYER: THE NORMAL VIDEO FEED ---
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> if (index < videos.size) videos[index].id else index },
            userScrollEnabled = !isScrubbing && activeAdOverride == null
        ) { page ->
            Box(modifier = Modifier
                .fillMaxSize()
                .clipToBounds()) {
                if (page < videos.size) {
                    val video = videos[page]
                    val player = playerPool.activePlayers[video.id]

                    if (player != null) {
                        ShortVideoPlayer(
                            video = video,
                            exoPlayer = player,
                            isActive = pagerState.currentPage == page && isAppForeground && activeAdOverride == null,
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

        // --- TOP LAYER: THE ADVERTISEMENT OVERLAY ---
        AnimatedVisibility(
            visible = activeAdOverride != null,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (activeAdOverride != null) {
                val player = playerPool.activePlayers[activeAdOverride!!.id]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { } }) {

                    LaunchedEffect(Unit) { onImmersiveChange(true) }

                    // FIX: Removed the erroneous `onLockChange` parameter invocation
                    AdPlayer(
                        ad = activeAdOverride!!,
                        exoPlayer = player,
                        isActive = isAppForeground,
                        onSkip = {
                            activeAdOverride = null
                        }
                    )
                }
            }
        }

        // End of Feed Toast
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