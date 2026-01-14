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

        val targets = getChaptersForAutoDownload.await()
        targets.forEach { (manga, chapters) ->
            // Use queue repository with NORMAL priority for reading history downloads
            val entries = chapters.map { chapter -> manga.id to chapter.id }
            downloadQueueRepository.addAll(entries, DownloadPriority.NORMAL.value)
        }

        // Start the download manager if it's not already running
        if (targets.isNotEmpty()) {
            downloadManager.startDownloads()
        }

        return Result.success()
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
