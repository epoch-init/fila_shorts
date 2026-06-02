package zeki.productions.shorts.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VideoEntity::class], version = 3, exportSchema = false)
abstract class ShortsDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}