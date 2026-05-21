package zeki.productions.shorts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import zeki.productions.shorts.data.ShortsDatabase
import zeki.productions.shorts.data.VideoEntity
import zeki.productions.shorts.ui.components.CategoryBar
import zeki.productions.shorts.ui.navigation.BottomNavItem
import zeki.productions.shorts.ui.screens.AboutScreen
import zeki.productions.shorts.ui.screens.ProfileScreen
import zeki.productions.shorts.ui.screens.SearchScreen
import zeki.productions.shorts.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    liveDb: ShortsDatabase,
    stableDb: ShortsDatabase?,
    onRefreshStable: suspend () -> Unit,
    onDeletePhysical: suspend () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf(emptyList<VideoEntity>()) }
    var selectedCategory by remember { mutableStateOf("All") }

    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categories = remember(videos) {
        val list = mutableListOf("All")
        if (videos.any { it.isFavorite }) list.add("Favorites")
        val tags = videos.flatMap { it.categories.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "All" && it != "Favorites" }
            .distinct().sorted()
        list.addAll(tags)
        list
    }

    val filteredVideos = remember(videos, selectedCategory) {
        when (selectedCategory) {
            "All" -> videos
            "Favorites" -> videos.filter { it.isFavorite }
            else -> videos.filter {
                it.categories.split(",").map { c -> c.trim() }.contains(selectedCategory)
            }
        }
    }

    LaunchedEffect(stableDb) {
        stableDb?.let { db ->
            db.videoDao().getAllActiveVideos().collectLatest { videos = it }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val items =
                        listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Settings)

                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(
                                    item.route
                                ) == true
                            } == true,
                            onClick = {
                                if (item == BottomNavItem.Settings) {
                                    showSettingsSheet = true
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = BottomNavItem.Home.route,
            Modifier.fillMaxSize()
        ) {
            composable(
                route = BottomNavItem.Home.route + "?videoId={videoId}",
                arguments = listOf(navArgument("videoId") {
                    defaultValue = ""; type = NavType.StringType
                })
            ) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("videoId")
                Box(modifier = Modifier.fillMaxSize()) {
                    FeedScreen(
                        videos = filteredVideos,
                        initialVideoId = targetId,
                        onVideoSeen = { id ->
                            scope.launch(Dispatchers.IO) {
                                liveDb.videoDao().incrementViewCount(id)
                            }
                        },
                        onToggleFavorite = { updatedVideo ->
                            scope.launch(Dispatchers.IO) {
                                liveDb.videoDao().updateVideo(updatedVideo)
                                onRefreshStable()
                            }
                        },
                        onAccountSelected = { accountName ->
                            navController.navigate("profile/$accountName")
                        }
                    )

                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        CategoryBar(categories, selectedCategory) { selectedCategory = it }
                    }
                }
            }

            composable(BottomNavItem.Search.route) {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    SearchScreen(
                        videos = videos,
                        onVideoSelected = { videoId ->
                            navController.navigate(BottomNavItem.Home.route + "?videoId=$videoId") {
                                popUpTo(BottomNavItem.Home.route) { inclusive = true }
                            }
                        },
                        onAccountSelected = { accountName ->
                            navController.navigate("profile/$accountName")
                        }
                    )
                }
            }

            // New Profile Route
            composable(
                route = "profile/{accountName}",
                arguments = listOf(navArgument("accountName") { type = NavType.StringType })
            ) { backStackEntry ->
                val accountName = backStackEntry.arguments?.getString("accountName") ?: ""
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ProfileScreen(
                        accountName = accountName,
                        allVideos = videos,
                        onBack = { navController.popBackStack() },
                        onVideoSelected = { videoId ->
                            navController.navigate(BottomNavItem.Home.route + "?videoId=$videoId") {
                                popUpTo(BottomNavItem.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF0A0000),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
            ) {
                SettingsScreen(
                    videoCount = videos.size,
                    onDeleteSeen = {
                        scope.launch(Dispatchers.IO) {
                            onDeletePhysical()
                            onRefreshStable()
                            showSettingsSheet = false
                        }
                    },
                    onNavigateToAbout = {
                        showSettingsSheet = false
                        navController.navigate("about")
                    }
                )
            }
        }
    }
}