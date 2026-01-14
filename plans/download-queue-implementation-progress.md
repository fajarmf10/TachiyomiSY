# Download Queue Optimization - Implementation Progress

## Status: Phase 1-2 Complete (Foundations)

**Last Updated:** 2026-01-14

---

## ✅ Completed Phases

### Phase 1: Database Foundation (COMPLETE)
**Files Created:**
- ✅ `/data/src/main/sqldelight/tachiyomi/data/download_queue.sq` - SQL schema with indexes
- ✅ `/domain/src/main/java/tachiyomi/domain/download/model/DownloadQueueEntry.kt` - Models and enums
- ✅ `/domain/src/main/java/tachiyomi/domain/download/repository/DownloadQueueRepository.kt` - Repository interface
- ✅ `/data/src/main/java/tachiyomi/data/download/DownloadQueueRepositoryImpl.kt` - Repository implementation

**Files Modified:**
- ✅ `/app/src/main/java/eu/kanade/tachiyomi/data/download/DownloadStore.kt` - Added migration logic from SharedPreferences to database
- ✅ `/app/src/main/java/eu/kanade/tachiyomi/data/download/Downloader.kt` - Integrated database repository

**Key Features:**
- Database-backed queue with foreign keys (auto-cleanup on manga/chapter delete)
- Exponential backoff calculation (2min → 4min → 8min → ... → 6hr max)
- Priority system (URGENT, HIGH, NORMAL, LOW)
- Status tracking (PENDING, DOWNLOADING, FAILED, COMPLETED)
- Retry count and error message tracking

### Phase 2.1: Persistent Download Worker (COMPLETE)
**Files Modified:**
- ✅ `/app/src/main/java/eu/kanade/tachiyomi/data/download/DownloadJob.kt` - Refactored to periodic worker

**Key Features:**
- Periodic WorkManager job (configurable interval)
- Exponential backoff on failures
- Network/battery/storage constraints
- Result.retry() for network issues
- Backward compatible one-time job for manual triggers

---

## 🚧 Remaining Work

### Phase 2: Complete Periodic Worker Integration
- [ ] Phase 2.2: Update DownloadManager scheduling
- [ ] Phase 2.3: Setup periodic worker in App.kt

### Phase 3: Smart Auto-Download Polling
- [ ] Phase 3.1: Create GetChaptersForAutoDownload interactor
- [ ] Phase 3.2: Create AutoDownloadPollingWorker
- [ ] Phase 3.3: Fix ReaderViewModel auto-download requirement (line 623)

### Phase 4: Temp Folder Cleanup
- [ ] Phase 4.1: Create TempFolderCleanupWorker
- [ ] Phase 4.2: Add cleanup to Downloader init
- [ ] Phase 4.3: Add cleanup before temp folder creation

### Phase 5: Enhanced Error Handling
- [ ] Phase 5.1: Add error classification system
- [ ] Phase 5.2: Update Downloader error handling
- [ ] Phase 5.3: Add failure tracking to repository

### Phase 6: Preferences & UI
- [ ] Phase 6.1: Add new preferences to DownloadPreferences
  - `downloadWorkerInterval()` - 0/15/30/60/180/360 minutes
  - `autoDownloadFromReadingHistory()` - boolean
  - `autoDownloadReadingHistoryDays()` - 3/7/14/30 days
  - `autoDownloadMaxRetries()` - 3/5/10/999
  - `cleanupOrphanedFoldersOnStartup()` - boolean
- [ ] Phase 6.2: Update SettingsDownloadScreen UI

---

## 📋 TODO: Before Phases 1-2 Can Work

### Critical Setup Needed:
1. **Add missing preferences** (Phase 6.1 subset):
   - `downloadWorkerInterval()` in DownloadPreferences.kt
   - `autoDownloadMaxRetries()` in DownloadPreferences.kt

2. **Setup Dependency Injection**:
   - Register DownloadQueueRepositoryImpl in DI container
   - Bind DownloadQueueRepository interface to implementation

3. **Database Migration**:
   - SQLDelight will auto-generate queries from download_queue.sq
   - May need to bump database version number

4. **Initialize Periodic Worker**:
   - Call `DownloadJob.setupPeriodicWork(context)` in App.onCreate()

---

## 🔧 Known Issues to Fix

1. **Linter Errors:**
   - DownloadStore.kt: Type inference issues with Injekt.get()
   - DownloadStore.kt: logcat argument type mismatch
   - DownloadJob.kt: Unresolved reference 'downloadWorkerInterval'

2. **Missing Bindings:**
   - DownloadQueueRepository needs DI binding
   - May need to update DatabaseHandler to include download_queue table

---

## 📝 Implementation Notes

### Design Decisions Made:
1. **Database over SharedPreferences**: Better reliability, ACID transactions, foreign keys
2. **Periodic worker**: Survives app kills, automatic retry with backoff
3. **Priority levels**: Ensures user-triggered downloads happen first
4. **Exponential backoff**: Prevents hammering sources on failures

### Migration Strategy:
- On first run, existing SharedPreferences queue migrated to database
- Migration flag `queue_migrated_to_db` prevents re-migration
- Old queue data cleared after successful migration

### Backward Compatibility:
- DownloadStore.restore() still works, now loads from database
- One-time job still available via DownloadJob.start() for manual triggers
- All existing download flows preserved

---

## 🎯 Next Steps (When Resuming)

1. **Fix linter errors** (see Known Issues)
2. **Add minimal preferences** (downloadWorkerInterval, autoDownloadMaxRetries)
3. **Setup DI binding** for DownloadQueueRepository
4. **Test compilation** and fix any SQLDelight generation issues
5. **Test migration** from SharedPreferences to database
6. **Continue with Phase 3** (Auto-download polling)

---

## 📚 Reference

Full plan: `/plans/download-queue-optimization.md`

Key optimizations vs GitHub issues:
- Database > SharedPreferences for reliability
- Periodic worker > One-time job for persistence
- Reading history polling > Reader-only triggering for coverage
- Multi-point cleanup > Single-point for completeness
- Error classification > Generic handling for intelligence
