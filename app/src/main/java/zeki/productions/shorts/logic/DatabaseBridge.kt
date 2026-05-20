package zeki.productions.shorts.logic

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import zeki.productions.shorts.data.ShortsDatabase
import java.io.File

object DatabaseBridge {
    private const val TAG = "GEMINI_DEBUG"
    private const val LIVE_NAME = "shorts_live.db"
    private const val STABLE_NAME = "shorts_stable.db"

    fun getLiveDb(context: Context): ShortsDatabase {
        Log.d(TAG, "Bridge: Initializing Live DB connection")
        return Room.databaseBuilder(context, ShortsDatabase::class.java, LIVE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    fun getStableDb(context: Context, liveDbInstance: ShortsDatabase): ShortsDatabase {
        val dbDir = context.getDatabasePath(LIVE_NAME).parentFile
        Log.d(TAG, "Bridge: Attempting Snapshot. DB Directory: ${dbDir?.absolutePath}")

        try {
            Log.d(TAG, "Bridge: Requesting FULL WAL Checkpoint...")
            val cursor = liveDbInstance.openHelper.writableDatabase.query(
                SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")
            )
            if (cursor.moveToFirst()) {
                val busy = cursor.getInt(0)
                val log = cursor.getInt(1)
                val checkpointed = cursor.getInt(2)
                Log.d(TAG, "Bridge: Checkpoint Result -> Busy: $busy, Log Frames: $log, Checkpointed: $checkpointed")
            }
            cursor.close()

            val liveFile = context.getDatabasePath(LIVE_NAME)
            val stableFile = context.getDatabasePath(STABLE_NAME)

            if (liveFile.exists()) {
                Log.d(TAG, "Bridge: Live file found (${liveFile.length()} bytes). Copying to Stable...")
                liveFile.copyTo(stableFile, overwrite = true)

                // FIX: Safely clone WAL & SHM to prevent lock issues / DB corruption
                val liveWal = File(dbDir, "$LIVE_NAME-wal")
                val stableWal = File(dbDir, "$STABLE_NAME-wal")
                if (liveWal.exists()) {
                    liveWal.copyTo(stableWal, overwrite = true)
                }

                val liveShm = File(dbDir, "$LIVE_NAME-shm")
                val stableShm = File(dbDir, "$STABLE_NAME-shm")
                if (liveShm.exists()) {
                    liveShm.copyTo(stableShm, overwrite = true)
                }

                Log.d(TAG, "Bridge: DB, WAL, and SHM safely cloned.")
            } else {
                Log.e(TAG, "Bridge: CRITICAL - Live file does not exist at ${liveFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bridge: Snapshot Failed", e)
        }

        return Room.databaseBuilder(context, ShortsDatabase::class.java, STABLE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}