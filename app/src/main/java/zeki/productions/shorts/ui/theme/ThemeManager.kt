package zeki.productions.shorts.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppTheme(val title: String) {
    OXBLOOD("Oxblood"),
    MIDNIGHT("Midnight"),
    FOREST("Forest"),
    AMETHYST("Amethyst"),
    LIGHT("Light Mode")
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("fila_theme_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(
        AppTheme.valueOf(
            prefs.getString("theme_mode", AppTheme.OXBLOOD.name) ?: AppTheme.OXBLOOD.name
        )
    )
    val currentTheme: StateFlow<AppTheme> = _currentTheme

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("theme_mode", theme.name).apply()
        _currentTheme.value = theme
    }
}