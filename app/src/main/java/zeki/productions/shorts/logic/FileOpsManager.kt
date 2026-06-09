package zeki.productions.shorts.logic

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zeki.productions.shorts.data.VideoEntity
import java.io.File
import java.io.RandomAccessFile

object FileOpsManager {

    fun syncFavoriteStorage(context: Context, video: VideoEntity, isFavorite: Boolean) {
        val rootInternal = context.getDir("favorites", Context.MODE_PRIVATE)
        val cat = video.categories.split(",").firstOrNull()?.trim() ?: "Uncategorized"
        val acc = video.accountName

        val targetDir = File(rootInternal, "$cat/$acc")

        if (isFavorite) {
            targetDir.mkdirs()
            val vFile = File(video.videoPath)
            val iFile = File(video.imagePath)
            val jFile = File(video.jsonPath)

            // Copy encrypted files to secure internal storage
            if (vFile.exists()) vFile.copyTo(
                File(targetDir, "${video.id}.mp4.short"),
                overwrite = true
            )
            if (iFile.exists()) iFile.copyTo(
                File(targetDir, "${video.id}.jpg.short"),
                overwrite = true
            )
            if (jFile.exists()) jFile.copyTo(
                File(targetDir, "${video.id}.info.json.short"),
                overwrite = true
            )
        } else {
            // Remove from internal storage if un-favorited
            File(targetDir, "${video.id}.mp4.short").delete()
            File(targetDir, "${video.id}.jpg.short").delete()
            File(targetDir, "${video.id}.info.json.short").delete()
        }
    }

    fun deleteSingleVideo(context: Context, video: VideoEntity) {
        val rootExternal = File(Environment.getExternalStorageDirectory(), "FILA TikTok")
        val rootInternal = context.getDir("favorites", Context.MODE_PRIVATE)

        val cat = video.categories.split(",").firstOrNull()?.trim() ?: "Uncategorized"
        val acc = video.accountName

        // Delete from BOTH external folder and internal cache
        listOf(rootExternal, rootInternal).forEach { baseDir ->
            val dir = File(baseDir, "$cat/$acc")
            File(dir, "${video.id}.mp4.short").delete()
            File(dir, "${video.id}.jpg.short").delete()
            File(dir, "${video.id}.info.json.short").delete()

            // Clean up empty creator directory
            if (dir.exists() && dir.listFiles()?.isEmpty() == true) dir.delete()
        }
    }

    suspend fun exportVideo(context: Context, video: VideoEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val inFile = File(video.videoPath)
                if (!inFile.exists()) return@withContext false

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "FILA_Export_${video.id}.mp4")
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/FILA Sports"
                    )
                }

                val uri =
                    resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        RandomAccessFile(inFile, "r").use { raf ->
                            val iv = ByteArray(CryptoConstants.IV_SIZE)
                            raf.readFully(iv)

                            val maxChunk = minOf(
                                CryptoConstants.HEADER_ENC_SIZE.toLong(),
                                inFile.length() - CryptoConstants.IV_SIZE
                            ).toInt()
                            val safeChunkSize = maxChunk - (maxChunk % 16)

                            if (safeChunkSize > 0) {
                                val encHeader = ByteArray(safeChunkSize)
                                raf.readFully(encHeader)
                                val decHeader = CryptoHelper.decryptHeader(encHeader, iv)
                                outStream.write(decHeader)
                            }

                            val buffer = ByteArray(128 * 1024)
                            var bytesRead: Int
                            while (raf.read(buffer).also { bytesRead = it } != -1) {
                                outStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext false
        }
}