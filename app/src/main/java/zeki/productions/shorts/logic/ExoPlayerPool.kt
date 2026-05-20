package zeki.productions.shorts.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import zeki.productions.shorts.data.VideoEntity
import java.io.File

/**
 * v1.9.4: Lifecycle-Aware Triple-Node Player Pool.
 * Optimized to handle global pause/resume signals to prevent background playback.
 */
@OptIn(UnstableApi::class)
class ExoPlayerPool(private val context: Context) {
    private val TAG = "GEMINI_DEBUG"
    private val poolSize = 3

    val activePlayers = mutableStateMapOf<String, ExoPlayer>()
    private val availablePlayers = ArrayDeque<ExoPlayer>()

    init {
        repeat(poolSize) {
            availablePlayers.add(createPlayer())
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    fun updateWindow(targetVideos: List<VideoEntity>, activeId: String, isAppForeground: Boolean) {
        val targetIds = targetVideos.map { it.id }.toSet()

        // 1. Evict expired players
        val toEvict = activePlayers.keys.filter { it !in targetIds }
        toEvict.forEach { id ->
            val player = activePlayers.remove(id)
            player?.let {
                it.stop()
                it.clearMediaItems()
                availablePlayers.add(it)
            }
        }

        // 2. Assign and Prepare new players
        targetVideos.forEach { video ->
            if (!activePlayers.containsKey(video.id)) {
                val player = availablePlayers.removeFirstOrNull() ?: createPlayer()
                val factory = StreamingDecryptDataSourceFactory(File(video.videoPath))
                val source = ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(MediaItem.fromUri(Uri.fromFile(File(video.videoPath))))

                player.setMediaSource(source)
                player.prepare()

                // Set initial playback state based on focus and app lifecycle
                player.playWhenReady = (video.id == activeId && isAppForeground)
                activePlayers[video.id] = player
            }
        }
    }

    /**
     * Sterilizes all active playback.
     * Called when the app moves to the background.
     */
    fun pauseAll() {
        activePlayers.values.forEach { it.playWhenReady = false }
    }

    fun release() {
        activePlayers.values.forEach { it.release() }
        availablePlayers.forEach { it.release() }
        activePlayers.clear()
        availablePlayers.clear()
        Log.d(TAG, "PlayerPool: Resources Sterilized.")
    }
}