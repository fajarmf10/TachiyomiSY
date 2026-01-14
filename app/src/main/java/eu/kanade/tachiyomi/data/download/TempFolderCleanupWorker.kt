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

    override suspend fun doWork(): Result {
        if (!downloadPreferences.cleanupOrphanedFoldersOnStartup().get()) return Result.success()
        cleanupOrphanedTempFolders()
        return Result.success()
    }

    companion object {
        private const val TAG = "TempFolderCleanup"

        suspend fun cleanupOrphanedTempFolders(
            maxAgeMillis: Long = TimeUnit.HOURS.toMillis(1),
        ): Int {
            val storageManager: StorageManager = Injekt.get()
            val downloadsDir = storageManager.getDownloadsDirectory() ?: return 0
            val cutoff = System.currentTimeMillis() - maxAgeMillis
            return cleanupInDirectory(downloadsDir, cutoff)
        }

        fun setupPeriodicWork(context: Context) {
            val preferences = Injekt.get<DownloadPreferences>()
            if (!preferences.cleanupOrphanedFoldersOnStartup().get()) {
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

        private fun cleanupInDirectory(dir: UniFile, cutoffMillis: Long): Int {
            var cleaned = 0
            dir.listFiles().orEmpty().forEach { file ->
                val name = file.name.orEmpty()
                if (name.endsWith(Downloader.TMP_DIR_SUFFIX) && file.lastModified() < cutoffMillis) {
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
