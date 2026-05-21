package zeki.productions.shorts.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import zeki.productions.shorts.data.VideoEntity

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
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000, 5000, 500, 1000
            ).build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    suspend fun updateWindow(
        targetVideos: List<VideoEntity>,
        activeId: String,
        isAppForeground: Boolean
    ) {
        val targetIds = targetVideos.map { it.id }.toSet()

        // 1. Evict expired players and clean up storage
        withContext(Dispatchers.Main) {
            val toEvict = activePlayers.keys.filter { it !in targetIds }
            toEvict.forEach { id ->
                val player = activePlayers.remove(id)
                player?.let {
                    it.stop()
                    it.clearMediaItems()
                    availablePlayers.add(it)
                }
            }
        }

        // Delete evicted files from the secure cache
        VideoCacheManager.cleanupIdle(context, targetIds)

        // 2. Prioritize the ACTIVE video first to minimize latency
        val activeVideo = targetVideos.find { it.id == activeId }
        if (activeVideo != null) {
            preparePlayerForVideo(activeVideo, isActive = true, isAppForeground)
        }

        // 3. Concurrently prepare the Next and Previous videos in the background
        coroutineScope {
            targetVideos.filter { it.id != activeId }.forEach { video ->
                launch {
                    preparePlayerForVideo(video, isActive = false, isAppForeground = false)
                }
            }
        }
    }

    private suspend fun preparePlayerForVideo(
        video: VideoEntity,
        isActive: Boolean,
        isAppForeground: Boolean
    ) {
        // Blocks until the video is safely cached in internal storage
        val cachedFile = VideoCacheManager.prepareVideo(context, video) ?: return

        withContext(Dispatchers.Main) {
            if (!activePlayers.containsKey(video.id)) {
                val player = availablePlayers.removeFirstOrNull() ?: createPlayer()

                // Standard File playback - Full Native Hardware Acceleration
                val mediaItem = MediaItem.fromUri(Uri.fromFile(cachedFile))
                player.setMediaItem(mediaItem)
                player.prepare()

                activePlayers[video.id] = player
            }

            if (isActive) {
                activePlayers[video.id]?.playWhenReady = isAppForeground
            }
        }
    }

    fun pauseAll() {
        activePlayers.values.forEach { it.playWhenReady = false }
    }

    fun release() {
        activePlayers.values.forEach { it.release() }
        availablePlayers.forEach { it.release() }
        activePlayers.clear()
        availablePlayers.clear()
        VideoCacheManager.sterilize(context) // Wipe temp files on exit
        Log.d(TAG, "PlayerPool: Resources Sterilized.")
    }
}