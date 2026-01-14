package tachiyomi.data.download

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.download.model.DownloadErrorType
import tachiyomi.domain.download.model.DownloadQueueEntry
import tachiyomi.domain.download.model.DownloadQueueStatus
import tachiyomi.domain.download.repository.DownloadQueueRepository
import tachiyomi.domain.download.service.DownloadPreferences
import kotlin.math.pow

class DownloadQueueRepositoryImpl(
    private val handler: DatabaseHandler,
    private val downloadPreferences: DownloadPreferences,
) : DownloadQueueRepository {

    override suspend fun getPendingByPriority(): List<DownloadQueueEntry> {
        return handler.awaitList {
            download_queueQueries.getPendingByPriority(mapper = ::mapDownloadQueueEntry)
        }
    }

    override suspend fun getPendingWithBackoff(): List<DownloadQueueEntry> {
        val all = getPendingByPriority()
        val now = System.currentTimeMillis()

        return all.filter { entry ->
            // Apply exponential backoff
            val lastAttempt = entry.lastAttemptAt ?: 0
            val backoffDelay = calculateBackoffDelay(entry.retryCount)

            now - lastAttempt >= backoffDelay
        }
    }

    private fun calculateBackoffDelay(retryCount: Int): Long {
        // Progressive backoff: 2min, 4min, 8min, 16min, 32min, ~1hr, ~2hr, ~4hr (capped at 6hr)
        val minutes = (2.0.pow(retryCount.coerceAtMost(7))).toLong() * 2
        return minutes.coerceAtMost(360) * 60 * 1000 // Max 6 hours
    }

    override suspend fun getAll(): List<DownloadQueueEntry> {
        return handler.awaitList {
            download_queueQueries.getAll(mapper = ::mapDownloadQueueEntry)
        }
    }

    override fun getAllAsFlow(): Flow<List<DownloadQueueEntry>> {
        return handler.subscribeToList {
            download_queueQueries.getAll(mapper = ::mapDownloadQueueEntry)
        }
    }

    override suspend fun getByChapterId(chapterId: Long): DownloadQueueEntry? {
        return handler.awaitOneOrNull {
            download_queueQueries.getByChapterId(chapterId, mapper = ::mapDownloadQueueEntry)
        }
    }

    override suspend fun getByMangaId(mangaId: Long): List<DownloadQueueEntry> {
        return handler.awaitList {
            download_queueQueries.getByMangaId(mangaId, mapper = ::mapDownloadQueueEntry)
        }
    }

    override suspend fun add(mangaId: Long, chapterId: Long, priority: Int): Long? {
        // Check if chapter is already in queue and insert atomically within a transaction
        // (per interface contract: return null if already exists)
        return handler.await(inTransaction = true) {
            // Check if entry already exists (inside transaction to prevent race condition)
            val existing = download_queueQueries.getByChapterId(
                chapterId,
                mapper = ::mapDownloadQueueEntry,
            ).executeAsOneOrNull()
            
            if (existing != null) {
                // Entry already exists, return null per interface contract
                null
            } else {
                // Insert new entry
                download_queueQueries.insert(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    priority = priority.toLong(),
                    addedAt = System.currentTimeMillis(),
                )
                // Fetch and return the newly inserted entry's ID
                download_queueQueries.getByChapterId(
                    chapterId,
                    mapper = ::mapDownloadQueueEntry,
                ).executeAsOneOrNull()?.id
            }
        }
    }

    override suspend fun addAll(entries: List<Pair<Long, Long>>, priority: Int) {
        handler.await(inTransaction = true) {
            val now = System.currentTimeMillis()
            entries.forEach { (mangaId, chapterId) ->
                download_queueQueries.insert(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    priority = priority.toLong(),
                    addedAt = now,
                )
            }
        }
    }

    override suspend fun updateStatus(
        id: Long,
        status: DownloadQueueStatus,
        lastAttemptAt: Long?,
        lastErrorMessage: String?,
    ) {
        handler.await {
            download_queueQueries.updateStatus(
                id = id,
                status = status.value,
                lastAttemptAt = lastAttemptAt,
                lastErrorMessage = lastErrorMessage,
            )
        }
    }

    override suspend fun recordFailure(
        chapterId: Long,
        errorMessage: String,
        errorType: DownloadErrorType,
    ) {
        handler.await {
            val entry = download_queueQueries.getByChapterId(
                chapterId,
                mapper = ::mapDownloadQueueEntry,
            ).executeAsOneOrNull() ?: return@await

            val newRetryCount = entry.retryCount + 1
            val maxRetries = downloadPreferences.autoDownloadMaxRetries().get()

            if (!errorType.canRetry || newRetryCount > maxRetries) {
                // Max retries exceeded or non-retryable error
                download_queueQueries.updateStatus(
                    id = entry.id,
                    status = DownloadQueueStatus.FAILED.value,
                    lastAttemptAt = System.currentTimeMillis(),
                    lastErrorMessage = errorMessage,
                )
            } else {
                // Can retry - update for exponential backoff
                download_queueQueries.updateForRetry(
                    id = entry.id,
                    lastAttemptAt = System.currentTimeMillis(),
                    lastErrorMessage = errorMessage,
                    retryCount = newRetryCount.toLong(),
                )
            }
        }
    }

    override suspend fun markCompleted(chapterId: Long) {
        handler.await {
            val entry = download_queueQueries.getByChapterId(
                chapterId,
                mapper = ::mapDownloadQueueEntry,
            ).executeAsOneOrNull() ?: return@await

            download_queueQueries.updateStatus(
                id = entry.id,
                status = DownloadQueueStatus.COMPLETED.value,
                lastAttemptAt = System.currentTimeMillis(),
                lastErrorMessage = null,
            )
        }
    }

    override suspend fun updatePriority(id: Long, priority: Int) {
        handler.await {
            download_queueQueries.updatePriority(
                id = id,
                priority = priority.toLong(),
            )
        }
    }

    override suspend fun removeByChapterId(chapterId: Long) {
        handler.await {
            download_queueQueries.removeByChapterId(chapterId)
        }
    }

    override suspend fun removeById(id: Long) {
        handler.await {
            download_queueQueries.removeById(id)
        }
    }

    override suspend fun removeByMangaId(mangaId: Long) {
        handler.await {
            download_queueQueries.removeByMangaId(mangaId)
        }
    }

    override suspend fun clearCompleted() {
        handler.await {
            download_queueQueries.clearCompleted()
        }
    }

    override suspend fun clearAll() {
        handler.await {
            download_queueQueries.clearAll()
        }
    }

    override suspend fun resetFailedToPending() {
        handler.await {
            download_queueQueries.resetFailedToPending()
        }
    }

    override suspend fun countByStatus(status: DownloadQueueStatus): Long {
        return handler.awaitOne {
            download_queueQueries.countByStatus(status.value)
        }
    }

    override suspend fun resetStuckDownloads(thresholdMillis: Long) {
        handler.await {
            download_queueQueries.resetStuckDownloads(thresholdMillis)
        }
    }

    private fun mapDownloadQueueEntry(
        _id: Long,
        manga_id: Long,
        chapter_id: Long,
        priority: Long,
        added_at: Long,
        retry_count: Long,
        last_attempt_at: Long?,
        last_error_message: String?,
        status: String,
    ): DownloadQueueEntry {
        return DownloadQueueEntry(
            id = _id,
            mangaId = manga_id,
            chapterId = chapter_id,
            priority = priority.toInt(),
            addedAt = added_at,
            retryCount = retry_count.toInt(),
            lastAttemptAt = last_attempt_at,
            lastErrorMessage = last_error_message,
            status = DownloadQueueStatus.fromString(status),
        )
    }
}
