package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchTitle
import tachiyomi.domain.manga.repository.MangaMetadataRepository

class GetSearchTitlesBatch(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {
    suspend fun await(mangaIds: List<Long>): Map<Long, List<SearchTitle>> {
        return mangaMetadataRepository.getTitlesByIds(mangaIds)
    }
}
