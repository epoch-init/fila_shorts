package zeki.productions.shorts.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import zeki.productions.shorts.ui.components.SwipeTutorialOverlay
import zeki.productions.shorts.ui.navigation.BottomNavItem
import zeki.productions.shorts.ui.screens.AboutScreen
import zeki.productions.shorts.ui.screens.CategoriesScreen
import zeki.productions.shorts.ui.screens.CreatorsScreen
import zeki.productions.shorts.ui.screens.FavoritesListScreen
import zeki.productions.shorts.ui.screens.ProfileScreen
import zeki.productions.shorts.ui.screens.SearchScreen
import zeki.productions.shorts.ui.screens.SettingsScreen
import zeki.productions.shorts.ui.theme.ThemeManager
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    liveDb: ShortsDatabase,
    stableDb: ShortsDatabase?,
    themeManager: ThemeManager,
    onRefreshStable: suspend () -> Unit,
    onDeletePhysical: suspend () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf(emptyList<VideoEntity>()) }
    var selectedCategory by remember { mutableStateOf("All") }

    var homeFeedSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // FIX: Global Immersive State (Hides UI when video is playing)
    var isImmersiveMode by remember { mutableStateOf(false) }

    // FIX: First-Time User Experience (Swipe Tutorial)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fila_prefs", Context.MODE_PRIVATE) }
    var showSwipeTutorial by remember { mutableStateOf(prefs.getBoolean("show_tutorial", true)) }

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

    val filteredVideos = remember(videos, selectedCategory, homeFeedSeed) {
        val baseList = when (selectedCategory) {
            "All" -> videos
            "Favorites" -> videos.filter { it.isFavorite }
            else -> videos.filter {
                it.categories.split(",").map { c -> c.trim() }.contains(selectedCategory)
            }
        }
        baseList.shuffled(Random(homeFeedSeed))
    }

    LaunchedEffect(stableDb) {
        stableDb?.let { db ->
            db.videoDao().getAllActiveVideos().collectLatest { videos = it }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route ?: ""

            val isVideoFeed = currentRoute.startsWith(BottomNavItem.Home.route) ||
                    currentRoute.startsWith("profile_feed") ||
                    currentRoute.startsWith("favorites_feed")

            val bottomNavGradientColor = if (isVideoFeed) {
                Color.Black.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            }

            val unselectedTint = if (isVideoFeed) {
                Color.White.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            }

            // FIX: Hide Bottom Navigation Bar in Immersive Mode
            AnimatedVisibility(
                visible = !isImmersiveMode || !isVideoFeed,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, bottomNavGradientColor)
                            )
                        )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        val items = listOf(
                            BottomNavItem.Home,
                            BottomNavItem.Categories,
                            BottomNavItem.Search,
                            BottomNavItem.Settings
                        )

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
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = unselectedTint,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = unselectedTint,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route + "?videoId={videoId}",
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

                composable(
                    route = BottomNavItem.Home.route + "?videoId={videoId}",
                    arguments = listOf(navArgument("videoId") {
                        defaultValue = ""; type = NavType.StringType
                    })
                ) { backStackEntry ->
                    val targetId = backStackEntry.arguments?.getString("videoId")

                    var showExitDialog by remember { mutableStateOf(false) }
                    BackHandler { showExitDialog = true }

                    if (showExitDialog) {
                        Dialog(onDismissRequest = { showExitDialog = false }) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        text = "Exit FILA Sports?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Are you sure you want to close the app?",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(32.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { showExitDialog = false }) {
                                            Text(
                                                "Cancel",
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = { (context as? Activity)?.finish() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Exit",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        key(selectedCategory, homeFeedSeed) {
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
                                },
                                onImmersiveChange = { isImmersiveMode = it } // Pass up state
                            )
                        }

                        // FIX: Hide Category Bar in Immersive Mode
                        AnimatedVisibility(
                            visible = !isImmersiveMode,
                            enter = slideInVertically { -it } + fadeIn(),
                            exit = slideOutVertically { -it } + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            CategoryBar(categories, selectedCategory) { newCategory ->
                                selectedCategory = newCategory
                                homeFeedSeed = System.currentTimeMillis()
                            }
                        }
                    }
                }

                composable(BottomNavItem.Categories.route) {
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        CategoriesScreen(
                            allVideos = videos,
                            onCategorySelected = { categoryName ->
                                navController.navigate("creators/$categoryName")
                            }
                        )
                    }
                }

                composable(
                    route = "creators/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        CreatorsScreen(
                            categoryName = categoryName,
                            allVideos = videos,
                            onBack = { navController.popBackStack() },
                            onAccountSelected = { accountName ->
                                navController.navigate("profile/$accountName")
                            },
                            onVideoSelected = { accountName, videoId ->
                                navController.navigate("profile_feed/$accountName?videoId=$videoId")
                            }
                        )
                    }
                }

                composable(BottomNavItem.Search.route) {
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        SearchScreen(
                            videos = videos,
                            onVideoSelected = { videoId ->
                                navController.navigate(BottomNavItem.Home.route + "?videoId=$videoId") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            onAccountSelected = { accountName ->
                                navController.navigate("profile/$accountName")
                            }
                        )
                    }
                }

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
                                navController.navigate("profile_feed/$accountName?videoId=$videoId")
                            }
                        )
                    }
                }

                composable(
                    route = "profile_feed/{accountName}?videoId={videoId}",
                    arguments = listOf(
                        navArgument("accountName") { type = NavType.StringType },
                        navArgument("videoId") { defaultValue = ""; type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val accountName = backStackEntry.arguments?.getString("accountName") ?: ""
                    val targetId = backStackEntry.arguments?.getString("videoId")

                    val accountVideos = remember(videos, accountName) {
                        videos.filter { it.accountName.equals(accountName, ignoreCase = true) }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        FeedScreen(
                            videos = accountVideos,
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
                            onAccountSelected = { navController.popBackStack() },
                            onImmersiveChange = { isImmersiveMode = it }
                        )

                        AnimatedVisibility(
                            visible = !isImmersiveMode,
                            enter = fadeIn(), exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                composable("favorites_list") {
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        FavoritesListScreen(
                            allVideos = videos,
                            onBack = { navController.popBackStack() },
                            onVideoSelected = { videoId ->
                                navController.navigate("favorites_feed?videoId=$videoId")
                            }
                        )
                    }
                }

                composable(
                    route = "favorites_feed?videoId={videoId}",
                    arguments = listOf(navArgument("videoId") {
                        defaultValue = ""; type = NavType.StringType
                    })
                ) { backStackEntry ->
                    val targetId = backStackEntry.arguments?.getString("videoId")

                    val favoriteVideos = remember(videos) {
                        videos.filter { it.isFavorite }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        FeedScreen(
                            videos = favoriteVideos,
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
                            },
                            onImmersiveChange = { isImmersiveMode = it }
                        )

                        AnimatedVisibility(
                            visible = !isImmersiveMode,
                            enter = fadeIn(), exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }

            // FIX: Inject the FTUE (First Time User Experience) Swipe Tutorial
            if (showSwipeTutorial && videos.isNotEmpty()) {
                SwipeTutorialOverlay(onDismiss = {
                    showSwipeTutorial = false
                    prefs.edit().putBoolean("show_tutorial", false).apply()
                })
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
            ) {
                SettingsScreen(
                    videoCount = videos.size,
                    themeManager = themeManager,
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
                    },
                    onNavigateToFavorites = {
                        showSettingsSheet = false
                        navController.navigate("favorites_list")
                    }
                )
            }
        }
    }
}