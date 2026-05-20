package zeki.productions.shorts.logic

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.io.RandomAccessFile
import kotlin.system.measureTimeMillis

@UnstableApi
class StreamingDecryptDataSource(private val file: File) : BaseDataSource(true) {
    private var raf: RandomAccessFile? = null
    private var headerCache: ByteArray? = null
    private var bytesRemaining: Long = 0
    private var position: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        val fileRaf = RandomAccessFile(file, "r")
        this.raf = fileRaf

        val time = measureTimeMillis {
            val iv = ByteArray(CryptoConstants.IV_SIZE)
            fileRaf.readFully(iv)

            // FIX: Enforce 16-byte bounds to avert IllegalBlockSizeException
            val maxChunk = minOf(CryptoConstants.HEADER_ENC_SIZE.toLong(), file.length() - CryptoConstants.IV_SIZE).toInt()
            val safeChunkSize = maxChunk - (maxChunk % 16)

            if (safeChunkSize > 0) {
                val encryptedHeader = ByteArray(safeChunkSize)
                val readHeader = fileRaf.read(encryptedHeader)

                if (readHeader > 0) {
                    val validRead = readHeader - (readHeader % 16)
                    if (validRead > 0) {
                        headerCache = CryptoHelper.decryptHeader(encryptedHeader.copyOf(validRead), iv)
                    }
                }
            }
        }

        if (time > 16) Log.w("LEAN_PERF", "Decryption Latency Warning: ${time}ms for ${file.name}")

        this.position = dataSpec.position
        val totalDataSize = file.length() - CryptoConstants.IV_SIZE
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
        else totalDataSize - dataSpec.position

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining <= 0L) return C.RESULT_END_OF_INPUT

        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        var bytesRead = 0

        // FIX: Replaced constant check with accurate runtime size evaluation
        val headerSize = headerCache?.size ?: 0

        if (position < headerSize) {
            val headerAvailable = (headerSize - position).toInt()
            val chunkFromHeader = minOf(toRead, headerAvailable)

            headerCache?.let {
                System.arraycopy(it, position.toInt(), buffer, offset, chunkFromHeader)
                bytesRead = chunkFromHeader
            }
        }

        if (bytesRead < toRead) {
            val remainingToRead = toRead - bytesRead
            val fileRequestPos = position + bytesRead + CryptoConstants.IV_SIZE

            raf?.let {
                it.seek(fileRequestPos)
                val readFromFile = it.read(buffer, offset + bytesRead, remainingToRead)
                if (readFromFile != -1) bytesRead += readFromFile
            }
        }

        if (bytesRead > 0) {
            position += bytesRead
            bytesRemaining -= bytesRead
            bytesTransferred(bytesRead)
            return bytesRead
        }

        return C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri = Uri.fromFile(file)

    override fun close() {
        raf?.close()
        raf = null
        headerCache = null
        transferEnded()
    }
}

class StreamingDecryptDataSourceFactory(private val file: File) : androidx.media3.datasource.DataSource.Factory {
    @UnstableApi
    override fun createDataSource() = StreamingDecryptDataSource(file)
}