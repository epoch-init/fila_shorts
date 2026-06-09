package zeki.productions.shorts.logic

import android.content.Context
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

    suspend fun sync(context: Context) {
        val externalPath =
            Paths.get(Environment.getExternalStorageDirectory().absolutePath, "FILA TikTok")
        val internalPath = context.getDir("favorites", Context.MODE_PRIVATE).toPath()

        val ledger = dao.getFullLedger().associateBy { it.id }
        val foundEntities = mutableMapOf<String, VideoEntity>()
        val discoveredIds = mutableSetOf<String>()

        fun scanDirectory(rootPath: Path) {
            if (!Files.exists(rootPath)) return

            Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val fileName = file.name

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
                        val extracted =
                            extractNewEntity(id, accountName, categoryName, file.toFile())

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
                            foundEntities[id] = updatedRecord
                        } else {
                            foundEntities[id] = extracted
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }

        // Scan both locations. Internal path processed second to guarantee paths resolve correctly if external is deleted.
        scanDirectory(externalPath)
        scanDirectory(internalPath)

        val toMarkDeleted = ledger.keys.filter { it !in discoveredIds && !ledger[it]!!.isDeleted }
        Log.d(
            TAG,
            "Indexer: Found ${foundEntities.size} to insert/update, ${toMarkDeleted.size} to mark deleted."
        )

        dao.syncLedger(foundEntities.values.toList(), toMarkDeleted)
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
            videoPath = if (vidFile.exists()) vidFile.absolutePath else "",
            jsonPath = jsonFile.absolutePath,
            imagePath = imgFile.absolutePath,
            description = desc,
            categories = cats,
            isAd = isAd,
            adType = adType
        )
    }
}