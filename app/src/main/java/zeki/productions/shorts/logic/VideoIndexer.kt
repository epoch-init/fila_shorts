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

                // We now check for json files to act as our index source of truth
                // because Image Ads do not have an .mp4.short file.
                if (fileName.endsWith(".info.json.short")) {
                    val id = fileName.substringBefore(".info.json.short")
                    discoveredIds.add(id)

                    val relPath = rootPath.relativize(file)
                    val depth = relPath.nameCount

                    val categoryName =
                        if (depth >= 3) relPath.getName(0).toString() else "Uncategorized"
                    val accountName = if (depth >= 3) relPath.getName(1)
                        .toString() else if (depth == 2) relPath.getName(0)
                        .toString() else "Unknown"

                    val historical = ledger[id]
                    val extracted = extractNewEntity(id, accountName, categoryName, file.toFile())

                    if (historical != null) {
                        val updatedRecord = historical.copy(
                            accountName = extracted.accountName,
                            categories = extracted.categories,
                            videoPath = extracted.videoPath,
                            imagePath = extracted.imagePath,
                            jsonPath = extracted.jsonPath,
                            description = extracted.description,
                            isAd = extracted.isAd,
                            adType = extracted.adType,
                            isDeleted = false
                        )

                        if (historical != updatedRecord) {
                            foundEntities.add(updatedRecord)
                        }
                    } else {
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
        jsonFile: File
    ): VideoEntity {
        val parentDir = jsonFile.parentFile
        val vidFile = File(parentDir, "$id.mp4.short")
        val imgFile = File(parentDir, "$id.jpg.short")

        var desc = "Imported File"
        var cats = fallbackCategory
        var isAd = false
        var adType = "video"

        try {
            val jsonStr = CryptoHelper.decryptFull(jsonFile.readBytes())
            if (jsonStr.isNotBlank()) {
                val json = JSONObject(jsonStr)
                desc = json.optString("description", "No Description")
                isAd = json.optBoolean("is_ad", false)
                adType = json.optString("ad_type", "video")

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

        return VideoEntity(
            id = id,
            accountName = account,
            videoPath = if (vidFile.exists()) vidFile.absolutePath else "", // Empty if image ad
            jsonPath = jsonFile.absolutePath,
            imagePath = imgFile.absolutePath,
            description = desc,
            categories = cats,
            isAd = isAd,
            adType = adType
        )
    }
}