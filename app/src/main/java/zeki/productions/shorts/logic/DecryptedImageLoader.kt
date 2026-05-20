package zeki.productions.shorts.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * v2.0.1: Memory-safe Image Loader.
 * Fixed NoPadding AES exceptions on unaligned file sizes.
 */
object DecryptedImageLoader {
    private const val TAG = "LEAN_IMG"
    private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB Limit

    suspend fun load(file: File): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() < CryptoConstants.IV_SIZE) return@withContext null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = file.length()

                val iv = ByteArray(CryptoConstants.IV_SIZE)
                raf.readFully(iv)

                // FIX: Truncate maxChunk to the nearest 16 bytes for NoPadding safety
                val maxChunk = minOf(CryptoConstants.HEADER_ENC_SIZE.toLong(), fileSize - CryptoConstants.IV_SIZE).toInt()
                val safeChunkSize = maxChunk - (maxChunk % 16)
                if (safeChunkSize <= 0) return@withContext null

                val encryptedHeader = ByteArray(safeChunkSize)
                raf.readFully(encryptedHeader)
                val decryptedHeader = CryptoHelper.decryptHeader(encryptedHeader, iv)

                val remainingSize = (fileSize - raf.filePointer).toInt()
                if (remainingSize + decryptedHeader.size > MAX_IMAGE_SIZE) {
                    Log.e(TAG, "File too large: ${file.name}")
                    return@withContext null
                }

                val completeData = ByteArray(decryptedHeader.size + remainingSize)
                System.arraycopy(decryptedHeader, 0, completeData, 0, decryptedHeader.size)
                if (remainingSize > 0) {
                    raf.readFully(completeData, decryptedHeader.size, remainingSize)
                }

                BitmapFactory.decodeByteArray(completeData, 0, completeData.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${file.name}", e)
            null
        }
    }
}