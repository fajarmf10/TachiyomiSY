package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchTag
import tachiyomi.domain.manga.repository.MangaMetadataRepository

class GetSearchTagsBatch(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {
    suspend fun await(mangaIds: List<Long>): Map<Long, List<SearchTag>> {
        return mangaMetadataRepository.getTagsByIds(mangaIds)
    }
}
