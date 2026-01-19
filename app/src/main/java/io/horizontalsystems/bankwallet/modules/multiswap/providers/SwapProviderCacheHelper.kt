package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.SwapProviderAssetRecord
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

object SwapProviderCacheHelper {
    private const val CACHE_LIFETIME_MS = 60 * 60 * 1000L // 1 hour

    private val dao = App.appDatabase.swapProviderAssetDao()

    suspend fun <T> getOrFetch(
        providerId: String,
        deserialize: (String) -> T?,
        serialize: (T) -> String,
        fetch: suspend () -> Map<Token, T>
    ): Map<Token, T> {
        val cached = getCachedData(providerId, deserialize)
        if (cached != null) {
            return cached
        }

        val data = fetch()
        saveToCache(providerId, data, serialize)
        return data
    }

    private fun <T> getCachedData(
        providerId: String,
        deserialize: (String) -> T?
    ): Map<Token, T>? {
        val oldestTimestamp = dao.getOldestTimestamp(providerId) ?: return null
        val cacheAge = System.currentTimeMillis() - oldestTimestamp

        if (cacheAge > CACHE_LIFETIME_MS) {
            dao.deleteByProvider(providerId)
            return null
        }

        val records = dao.getByProvider(providerId)
        if (records.isEmpty()) return null

        val queries = records.mapNotNull { TokenQuery.fromId(it.tokenQueryId) }
        val tokensMap = App.marketKit.tokens(queries).associate { it.tokenQuery.id to it }

        val result = mutableMapOf<Token, T>()
        for (record in records) {
            val token = tokensMap[record.tokenQueryId] ?: continue
            val data = deserialize(record.data) ?: continue
            result[token] = data
        }

        return result.ifEmpty { null }
    }

    private fun <T> saveToCache(
        providerId: String,
        data: Map<Token, T>,
        serialize: (T) -> String
    ) {
        val timestamp = System.currentTimeMillis()
        val records = data.map { (token, value) ->
            SwapProviderAssetRecord(
                providerId = providerId,
                tokenQueryId = token.tokenQuery.id,
                data = serialize(value),
                timestamp = timestamp
            )
        }

        dao.deleteByProvider(providerId)
        dao.insertAll(records)
    }
}
