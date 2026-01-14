package tachiyomi.domain.download.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.download.model.DownloadErrorType
import tachiyomi.domain.download.model.DownloadPriority
import tachiyomi.domain.download.model.DownloadQueueEntry
import tachiyomi.domain.download.model.DownloadQueueStatus

interface DownloadQueueRepository {

    /**
     * Get all pending downloads ordered by priority
     */
    suspend fun getPendingByPriority(): List<DownloadQueueEntry>

    /**
     * Get pending downloads with exponential backoff applied
     */
    suspend fun getPendingWithBackoff(): List<DownloadQueueEntry>

    /**
     * Get all downloads (any status)
     */
    suspend fun getAll(): List<DownloadQueueEntry>

    /**
     * Get all downloads as a Flow
     */
    fun getAllAsFlow(): Flow<List<DownloadQueueEntry>>

    /**
     * Get download by chapter ID
     */
    suspend fun getByChapterId(chapterId: Long): DownloadQueueEntry?

    /**
     * Get downloads by manga ID
     */
    suspend fun getByMangaId(mangaId: Long): List<DownloadQueueEntry>

    /**
     * Add a new download to the queue
     * Returns the entry ID, or null if chapter already in queue
     */
    suspend fun add(
        mangaId: Long,
        chapterId: Long,
        priority: Int = DownloadPriority.NORMAL.value,
    ): Long?

    /**
     * Add multiple downloads to the queue
     */
    suspend fun addAll(entries: List<Pair<Long, Long>>, priority: Int = DownloadPriority.NORMAL.value)

    /**
     * Update download status
     */
    suspend fun updateStatus(
        id: Long,
        status: DownloadQueueStatus,
        lastAttemptAt: Long? = null,
        lastErrorMessage: String? = null,
    )

    /**
     * Record a download failure and increment retry count
     */
    suspend fun recordFailure(
        chapterId: Long,
        errorMessage: String,
        errorType: DownloadErrorType,
    )

    /**
     * Mark a download as completed
     */
    suspend fun markCompleted(chapterId: Long)

    /**
     * Update download priority
     */
    suspend fun updatePriority(id: Long, priority: Int)

    /**
     * Remove download by chapter ID
     */
    suspend fun removeByChapterId(chapterId: Long)

    /**
     * Remove download by ID
     */
    suspend fun removeById(id: Long)

    /**
     * Remove all downloads by manga ID
     */
    suspend fun removeByMangaId(mangaId: Long)

    /**
     * Clear all completed downloads
     */
    suspend fun clearCompleted()

    /**
     * Clear all downloads
     */
    suspend fun clearAll()

    /**
     * Reset failed downloads to pending
     */
    suspend fun resetFailedToPending()

    /**
     * Get count by status
     */
    suspend fun countByStatus(status: DownloadQueueStatus): Long

    /**
     * Reset stuck downloads (in DOWNLOADING status for too long)
     */
    suspend fun resetStuckDownloads(thresholdMillis: Long = 30 * 60 * 1000) // 30 minutes
}
