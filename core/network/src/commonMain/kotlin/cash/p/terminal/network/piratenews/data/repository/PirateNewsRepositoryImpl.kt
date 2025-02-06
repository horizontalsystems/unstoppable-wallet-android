package cash.p.terminal.network.piratenews.data.repository

import cash.p.terminal.network.piratenews.api.PirateNewsApi
import cash.p.terminal.network.piratenews.data.mapper.PirateNewsMapper
import cash.p.terminal.network.piratenews.domain.repository.PirateNewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PirateNewsRepositoryImpl(
    private val placeApi: PirateNewsApi,
    private val pirateNewsMapper: PirateNewsMapper
) : PirateNewsRepository {
    override suspend fun getNews() =
        withContext(Dispatchers.IO) {
            pirateNewsMapper.mapNews(placeApi.getNews())
        }
}