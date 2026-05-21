package zeki.productions.shorts

import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import zeki.productions.shorts.data.ShortsDatabase
import zeki.productions.shorts.logic.DatabaseBridge
import zeki.productions.shorts.logic.PermissionManager
import zeki.productions.shorts.logic.SyncManager
import zeki.productions.shorts.logic.VideoCacheManager
import zeki.productions.shorts.ui.MainScreen
import zeki.productions.shorts.ui.screens.PermissionRequestScreen
import zeki.productions.shorts.ui.theme.ShortsTheme
import zeki.productions.shorts.ui.theme.ThemeManager
import java.io.File

class MainActivity : ComponentActivity() {
    private var liveDb: ShortsDatabase? = null
    private var stableDbState = mutableStateOf<ShortsDatabase?>(null)
    private lateinit var permissionManager: PermissionManager
    private var syncManager: SyncManager? = null
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        super.onCreate(savedInstanceState)

        VideoCacheManager.sterilize(this)
        liveDb = DatabaseBridge.getLiveDb(this)
        permissionManager = PermissionManager(this)
        themeManager = ThemeManager(applicationContext)

        syncManager = SyncManager(this, liveDb!!) { freshStable ->
            stableDbState.value = freshStable
        }

        setContent {
            val currentTheme by themeManager.currentTheme.collectAsState()

            ShortsTheme(theme = currentTheme) {
                var isAccessGranted by remember { mutableStateOf(permissionManager.isFullyGranted()) }
                val stableDb by stableDbState

                DisposableEffect(this@MainActivity) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val granted = permissionManager.isFullyGranted()
                            if (granted != isAccessGranted) isAccessGranted = granted
                            if (isAccessGranted) syncManager?.startProactiveSync()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                if (!isAccessGranted) {
                    PermissionRequestScreen { permissionManager.startPermissionFlow() }
                } else {
                    MainScreen(
                        liveDb = liveDb!!,
                        stableDb = stableDb,
                        themeManager = themeManager, // Passed to MainScreen
                        onRefreshStable = {
                            val fresh = DatabaseBridge.getStableDb(this@MainActivity, liveDb!!)
                            stableDbState.value = fresh
                        },
                        onDeletePhysical = { performPhysicalDeletion() }
                    )
                }
            }
        }
    }

    private suspend fun performPhysicalDeletion() {
        val dao = liveDb?.videoDao() ?: return
        val rootShortsDir = File(Environment.getExternalStorageDirectory(), "Shorts").absolutePath

        dao.getSeenActiveVideos().filter { !it.isFavorite }.forEach { video ->
            File(video.videoPath).delete()
            File(video.jsonPath).delete()
            File(video.imagePath).delete()

            val parentDir = File(video.videoPath).parentFile
            if (parentDir != null &&
                parentDir.absolutePath != rootShortsDir &&
                parentDir.isDirectory &&
                parentDir.listFiles()?.isEmpty() == true
            ) {
                parentDir.delete()
            }

            dao.markAsDeleted(video.id)
        }
    }

    override fun onDestroy() {
        syncManager?.cancel()
        VideoCacheManager.sterilize(this)
        super.onDestroy()
    }
}