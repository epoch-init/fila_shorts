package zeki.productions.shorts.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["viewedCount"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isDeleted"]),
        Index(value = ["timestamp"])
    ]
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val accountName: String,
    val videoPath: String,
    val jsonPath: String,
    val imagePath: String,
    val description: String,
    val categories: String,
    val viewedCount: Int = 0,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val isAd: Boolean = false,
    val adType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)