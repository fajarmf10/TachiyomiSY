package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy

// SY -->
/**
 * Manages the session-based unlock state for locked categories.
 * Categories remain unlocked until the app process terminates or timeout occurs.
 */
object CategoryLockManager {
    private val securityPreferences: SecurityPreferences by injectLazy()

    // Set of category IDs that are currently unlocked in this session
    private val unlockedCategories = mutableSetOf<Long>()

    // Map of category ID to unlock timestamp (in milliseconds)
    private val unlockTimestamps = mutableMapOf<Long, Long>()

    /**
     * Check if a category is currently unlocked in this session
     */
    fun isUnlocked(categoryId: Long): Boolean {
        checkTimeouts()
        return unlockedCategories.contains(categoryId)
    }

    /**
     * Mark a category as unlocked for this session
     */
    fun unlock(categoryId: Long) {
        unlockedCategories.add(categoryId)
        unlockTimestamps[categoryId] = System.currentTimeMillis()
    }

    /**
     * Lock a specific category (remove from unlocked set)
     */
    fun lock(categoryId: Long) {
        unlockedCategories.remove(categoryId)
        unlockTimestamps.remove(categoryId)
    }

    /**
     * Lock all categories
     */
    fun lockAll() {
        unlockedCategories.clear()
        unlockTimestamps.clear()
    }

    /**
     * Check if any unlocked categories have exceeded the timeout and lock them
     */
    private fun checkTimeouts() {
        val timeoutMinutes = securityPreferences.categoryLockTimeout().get()

        // 0 means never re-lock during session, -1 means always require PIN (no session unlock)
        if (timeoutMinutes <= 0) return

        val timeoutMillis = timeoutMinutes * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val categoriesToLock = unlockTimestamps.filter { (_, timestamp) ->
            currentTime - timestamp > timeoutMillis
        }.keys

        categoriesToLock.forEach { categoryId ->
            lock(categoryId)
        }
    }

    /**
     * Get all currently unlocked category IDs
     */
    fun getUnlockedCategories(): Set<Long> {
        checkTimeouts()
        return unlockedCategories.toSet()
    }
}
// SY <--
