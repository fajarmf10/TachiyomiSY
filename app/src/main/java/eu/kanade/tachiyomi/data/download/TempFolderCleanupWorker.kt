package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hippo.unifile.UniFile
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class TempFolderCleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val downloadPreferences: DownloadPreferences = Injekt.get()

    /**
     * Triggers orphaned temporary folder cleanup when the worker runs and the "cleanup on startup" preference is enabled.
     *
     * The worker always reports success to WorkManager regardless of whether cleanup was performed or any items were removed.
     *
     * @return `Result.success()` indicating the work completed.
     */
    override suspend fun doWork(): Result {
        if (!downloadPreferences.cleanupOrphanedFoldersOnStartup().get()) return Result.success()
        cleanupOrphanedTempFolders()
        return Result.success()
    }

    companion object {
        private const val TAG = "TempFolderCleanup"

        /**
         * Removes orphaned temporary download folders that are older than the specified age.
         *
         * @param maxAgeMillis Age threshold in milliseconds; folders last modified earlier than
         *                     (current time - maxAgeMillis) are considered orphaned and eligible for deletion.
         * @return The number of temporary folders deleted.
         */
        suspend fun cleanupOrphanedTempFolders(
            maxAgeMillis: Long = TimeUnit.HOURS.toMillis(1),
        ): Int {
            val storageManager: StorageManager = Injekt.get()
            val downloadsDir = storageManager.getDownloadsDirectory() ?: return 0
            val cutoff = System.currentTimeMillis() - maxAgeMillis
            return cleanupInDirectory(downloadsDir, cutoff)
        }

        /**
         * Schedules or cancels a periodic background task that cleans up orphaned temporary download folders.
         *
         * When enabled (either via the optional `enabled` parameter or the user's preference), enqueues a daily
         * PeriodicWorkRequest (with a 2-hour flex window and exponential backoff) for TempFolderCleanupWorker;
         * when not enabled, cancels any existing periodic work with the worker's unique tag.
         *
         * @param enabled If non-null, overrides the stored preference and controls whether the periodic cleanup is scheduled.
         */
        fun setupPeriodicWork(context: Context, enabled: Boolean? = null) {
            val preferences = Injekt.get<DownloadPreferences>()
            val isEnabled = enabled ?: preferences.cleanupOrphanedFoldersOnStartup().get()
            if (!isEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(TAG)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<TempFolderCleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = 2,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.MINUTES,
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /**
         * Recursively deletes orphaned temporary download folders under the given directory that are older than the cutoff.
         *
         * @param dir The directory to scan for temporary folders.
         * @param cutoffMillis Millisecond timestamp; folders with a positive `lastModified` earlier than this value will be removed.
         * @return The number of temporary folders successfully deleted.
         */
        private fun cleanupInDirectory(dir: UniFile, cutoffMillis: Long): Int {
            var cleaned = 0
            dir.listFiles().orEmpty().forEach { file ->
                val name = file.name.orEmpty()
                val lastMod = file.lastModified()
                if (name.endsWith(Downloader.TMP_DIR_SUFFIX) && lastMod > 0 && lastMod < cutoffMillis) {
                    if (file.delete()) {
                        cleaned += 1
                    }
                } else if (file.isDirectory) {
                    cleaned += cleanupInDirectory(file, cutoffMillis)
                }
            }
            return cleaned
        }
    }
}
