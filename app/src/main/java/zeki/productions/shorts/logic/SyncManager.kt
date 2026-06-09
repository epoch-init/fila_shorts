package zeki.productions.shorts.logic

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import zeki.productions.shorts.data.ShortsDatabase

class SyncManager(
    private val context: Context,
    private val liveDb: ShortsDatabase,
    private val onSyncComplete: (ShortsDatabase) -> Unit
) {
    private val TAG = "GEMINI_DEBUG"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startProactiveSync() {
        scope.launch {
            try {
                Log.d(TAG, "SyncManager: Proactive synchronization initiated.")
                val indexer = VideoIndexer(liveDb.videoDao())
                indexer.sync(context) // FIX: Passing Context

                val stableDb = DatabaseBridge.getStableDb(context, liveDb)
                withContext(Dispatchers.Main) {
                    onSyncComplete(stableDb)
                    Log.d(TAG, "SyncManager: Proactive synchronization complete.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "SyncManager: Synchronization failure", e)
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }
}