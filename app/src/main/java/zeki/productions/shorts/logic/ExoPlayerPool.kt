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

        VideoCacheManager.cleanupIdle(context, targetIds)

        val activeVideo = targetVideos.find { it.id == activeId }
        if (activeVideo != null) {
            preparePlayerForVideo(activeVideo, isActive = true, isAppForeground)
        }

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
        // FIX: Ignore Image Ads (they have no video file to cache or play)
        if (video.videoPath.isBlank()) return

        val cachedFile = VideoCacheManager.prepareVideo(context, video) ?: return

        withContext(Dispatchers.Main) {
            if (!activePlayers.containsKey(video.id)) {
                val player = availablePlayers.removeFirstOrNull() ?: createPlayer()

                val mediaItem = MediaItem.fromUri(Uri.fromFile(cachedFile))
                player.setMediaItem(mediaItem)
                player.prepare()

                player.playWhenReady = isActive && isAppForeground
                activePlayers[video.id] = player
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
        VideoCacheManager.sterilize(context)
        Log.d(TAG, "PlayerPool: Resources Sterilized.")
    }
}