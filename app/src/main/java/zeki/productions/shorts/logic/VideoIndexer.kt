package zeki.productions.shorts.logic

import android.os.Environment
import android.util.Log
import org.json.JSONObject
import zeki.productions.shorts.data.VideoDao
import zeki.productions.shorts.data.VideoEntity
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

/**
 * v1.8.4: NIO FileWalker Implementation.
 * Fixed massive DB rewrite inefficiency by skipping unmodified active records.
 */
class VideoIndexer(private val dao: VideoDao) {
    private val TAG = "GEMINI_DEBUG"

    suspend fun sync() {
        val rootPath = Paths.get(Environment.getExternalStorageDirectory().absolutePath, "Shorts")
        Log.d(TAG, "Indexer: Starting NIO walk at $rootPath")

        if (!Files.exists(rootPath)) {
            Log.e(TAG, "Indexer: Root path missing at ${rootPath.toAbsolutePath()}")
            return
        }

        val ledger = dao.getFullLedger().associateBy { it.id }
        val foundEntities = mutableListOf<VideoEntity>()
        val discoveredIds = mutableSetOf<String>()

        Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileName = file.name
                if (fileName.endsWith(".mp4.short")) {
                    val id = fileName.substringBefore(".mp4.short")
                    discoveredIds.add(id)

                    val userDir = file.parent
                    val accountName = userDir?.name ?: "Unknown"
                    val historical = ledger[id]

                    if (historical != null) {
                        // FIX: Only stage for update if it requires state mutation
                        if (historical.isDeleted) {
                            foundEntities.add(historical.copy(isDeleted = false))
                        }
                    } else {
                        foundEntities.add(extractNewEntity(id, accountName, file.toFile()))
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })

        val toMarkDeleted = ledger.keys.filter { it !in discoveredIds && !ledger[it]!!.isDeleted }
        Log.d(TAG, "Indexer: Found ${foundEntities.size} to insert/restore, ${toMarkDeleted.size} to mark deleted.")

        dao.syncLedger(foundEntities, toMarkDeleted)
    }

    private fun extractNewEntity(id: String, account: String, videoFile: File): VideoEntity {
        val parentDir = videoFile.parentFile
        val infoFile = File(parentDir, "$id.info.json.short")
        val imgFile = File(parentDir, "$id.jpg.short")

        var desc = "Imported Short"
        var cats = "All"

        if (infoFile.exists()) {
            try {
                val jsonStr = CryptoHelper.decryptFull(infoFile.readBytes())
                if (jsonStr.isNotBlank()) {
                    val json = JSONObject(jsonStr)
                    desc = json.optString("description", "No Description")

                    val categoriesArray = json.optJSONArray("categories")
                    if (categoriesArray != null && categoriesArray.length() > 0) {
                        val catList = mutableListOf<String>()
                        for (i in 0 until categoriesArray.length()) {
                            catList.add(categoriesArray.getString(i))
                        }
                        cats = catList.joinToString(",")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Indexer: JSON extraction failed for $id", e)
            }
        }

        return VideoEntity(
            id = id,
            accountName = account,
            videoPath = videoFile.absolutePath,
            jsonPath = infoFile.absolutePath,
            imagePath = imgFile.absolutePath,
            description = desc,
            categories = cats
        )
    }
}