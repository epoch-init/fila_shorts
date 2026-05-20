package zeki.productions.shorts.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * v1.8.2: Incremented version to 2.
 * Schema change: Added indices for O(log N) retrieval.
 */
@Database(entities = [VideoEntity::class], version = 2, exportSchema = false)
abstract class ShortsDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}