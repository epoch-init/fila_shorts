package zeki.productions.shorts.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY viewedCount ASC")
    fun getAllActiveVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos")
    suspend fun getFullLedger(): List<VideoEntity>

    @Query("UPDATE videos SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: String)

    @Query("UPDATE videos SET viewedCount = viewedCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("SELECT * FROM videos WHERE viewedCount > 0 AND isDeleted = 0 AND isFavorite = 0")
    suspend fun getSeenActiveVideos(): List<VideoEntity>

    @Transaction
    suspend fun syncLedger(toInsert: List<VideoEntity>, toMarkDeleted: List<String>) {
        if (toInsert.isNotEmpty()) insertVideos(toInsert)
        toMarkDeleted.forEach { markAsDeleted(it) }
    }
}