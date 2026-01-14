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
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.interactor.GetChaptersForAutoDownload
import tachiyomi.domain.download.model.DownloadPriority
import tachiyomi.domain.download.repository.DownloadQueueRepository
import tachiyomi.domain.download.service.DownloadPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class AutoDownloadPollingWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val downloadManager: DownloadManager = Injekt.get()
    private val downloadPreferences: DownloadPreferences = Injekt.get()
    private val getChaptersForAutoDownload: GetChaptersForAutoDownload = Injekt.get()
    private val downloadQueueRepository: DownloadQueueRepository = Injekt.get()

    override suspend fun doWork(): Result {
        if (!downloadPreferences.autoDownloadFromReadingHistory().get()) return Result.success()

        return try {
            val targets = getChaptersForAutoDownload.await()

            // Collect ALL entries first, then add in one batch (more efficient!)
            val allEntries = targets.flatMap { (manga, chapters) ->
                chapters.map { chapter -> manga.id to chapter.id }
            }

            if (allEntries.isNotEmpty()) {
                downloadQueueRepository.addAll(allEntries, DownloadPriority.NORMAL.value)
                downloadManager.startDownloads()
            }

            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Failed to auto-download chapters: ${e.message}" }
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutoDownloadPolling"

        fun setupPeriodicWork(context: Context, enabled: Boolean? = null) {
            val preferences = Injekt.get<DownloadPreferences>()
            val isEnabled = enabled ?: preferences.autoDownloadFromReadingHistory().get()
            if (!isEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(TAG)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (preferences.downloadOnlyOverWifi().get())
                        NetworkType.UNMETERED  // WiFi only
                    else
                        NetworkType.CONNECTED   // Any network
                )
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoDownloadPollingWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
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
    }
}
