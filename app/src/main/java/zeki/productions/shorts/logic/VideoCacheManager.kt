package zeki.productions.shorts.logic

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import zeki.productions.shorts.data.VideoEntity
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Secure Temporary Playback Cache.
 * Decrypts videos into the app's sandboxed internal storage for hardware-accelerated playback.
 * Files in this directory are physically inaccessible to other apps or the user.
 */
object VideoCacheManager {
    private const val TAG = "LEAN_CACHE"
    private val processingJobs = ConcurrentHashMap<String, Job>()
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun getSecureDir(context: Context): File {
        val dir = File(context.cacheDir, "secure_playback")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun prepareVideo(context: Context, video: VideoEntity): File? {
        val secureDir = getSecureDir(context)
        val outFile = File(secureDir, "${video.id}.mp4")

        // If it's already fully cached, return immediately
        if (outFile.exists() && outFile.length() > 0) {
            return outFile
        }

        // If it's currently being decrypted, wait for it to finish
        processingJobs[video.id]?.join()
        if (outFile.exists() && outFile.length() > 0) {
            return outFile
        }

        // Otherwise, decrypt it now
        val job = cacheScope.launch {
            try {
                val inFile = File(video.videoPath)
                if (!inFile.exists()) return@launch

                RandomAccessFile(inFile, "r").use { raf ->
                    FileOutputStream(outFile).use { fos ->
                        // 1. Extract IV
                        val iv = ByteArray(CryptoConstants.IV_SIZE)
                        raf.readFully(iv)

                        // 2. Decrypt Header
                        val maxChunk = minOf(
                            CryptoConstants.HEADER_ENC_SIZE.toLong(),
                            inFile.length() - CryptoConstants.IV_SIZE
                        ).toInt()
                        val safeChunkSize = maxChunk - (maxChunk % 16)

                        if (safeChunkSize > 0) {
                            val encHeader = ByteArray(safeChunkSize)
                            raf.readFully(encHeader)
                            val decHeader = CryptoHelper.decryptHeader(encHeader, iv)
                            fos.write(decHeader)
                        }

                        // 3. Fast-Stream the remaining plain text bytes
                        val buffer = ByteArray(128 * 1024) // 128KB buffer for rapid disk writing
                        var bytesRead: Int
                        while (raf.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache video ${video.id}", e)
                if (outFile.exists()) outFile.delete() // Clean up corrupted file
            }
        }

        processingJobs[video.id] = job
        job.join()
        processingJobs.remove(video.id)

        return if (outFile.exists()) outFile else null
    }

    /**
     * Deletes all files NOT in the active window to conserve storage.
     */
    fun cleanupIdle(context: Context, activeIds: Set<String>) {
        cacheScope.launch {
            val secureDir = getSecureDir(context)
            secureDir.listFiles()?.forEach { file ->
                val videoId = file.nameWithoutExtension
                if (videoId !in activeIds) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Sterilizes the entire cache folder (called on app startup/shutdown).
     */
    fun sterilize(context: Context) {
        val secureDir = getSecureDir(context)
        secureDir.listFiles()?.forEach { it.delete() }
        processingJobs.clear()
        Log.d(TAG, "Secure cache completely sterilized.")
    }
}