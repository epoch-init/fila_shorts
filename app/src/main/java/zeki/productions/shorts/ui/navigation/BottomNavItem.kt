package zeki.productions.shorts.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Categories : BottomNavItem("categories", Icons.Default.List, "Categories") // UPDATED TAB
    object Search : BottomNavItem("search", Icons.Default.Search, "Search")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}