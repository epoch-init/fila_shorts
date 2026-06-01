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

                    val relPath = rootPath.relativize(file)
                    val depth = relPath.nameCount

                    // Depth 3 = Tech/MKBHD/video.mp4.short
                    val categoryName =
                        if (depth >= 3) relPath.getName(0).toString() else "Uncategorized"
                    val accountName = if (depth >= 3) relPath.getName(1)
                        .toString() else if (depth == 2) relPath.getName(0)
                        .toString() else "Unknown"

                    val historical = ledger[id]
                    val extracted = extractNewEntity(id, accountName, categoryName, file.toFile())

                    if (historical != null) {
                        // FIX: Update the existing DB record with the NEW paths and categories
                        // in case the user moved the folders, but preserve user stats (likes/views).
                        val updatedRecord = historical.copy(
                            accountName = extracted.accountName,
                            categories = extracted.categories,
                            videoPath = extracted.videoPath,
                            imagePath = extracted.imagePath,
                            jsonPath = extracted.jsonPath,
                            description = extracted.description,
                            isDeleted = false
                        )

                        // Only add to the update queue if something actually changed
                        if (historical != updatedRecord) {
                            foundEntities.add(updatedRecord)
                        }
                    } else {
                        // Brand new video
                        foundEntities.add(extracted)
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })

        val toMarkDeleted = ledger.keys.filter { it !in discoveredIds && !ledger[it]!!.isDeleted }
        Log.d(
            TAG,
            "Indexer: Found ${foundEntities.size} to insert/update, ${toMarkDeleted.size} to mark deleted."
        )

        dao.syncLedger(foundEntities, toMarkDeleted)
    }

    private fun extractNewEntity(
        id: String,
        account: String,
        fallbackCategory: String,
        videoFile: File
    ): VideoEntity {
        val parentDir = videoFile.parentFile
        val infoFile = File(parentDir, "$id.info.json.short")
        val imgFile = File(parentDir, "$id.jpg.short")

        var desc = "Imported Short"
        var cats = fallbackCategory // Uses the new Major Category folder name

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