package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.SwapProviderAssetRecord
import io.horizontalsystems.bankwallet.entities.SwapProviderChainRecord
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

object SwapProviderCacheHelper {
    private const val CACHE_LIFETIME_MS = 60 * 60 * 1000L // 1 hour

    private val dao = App.appDatabase.swapProviderAssetDao()
    private val chainDao = App.appDatabase.swapProviderChainDao()

    suspend fun <T> getOrFetch(
        providerId: String,
        deserialize: (String) -> T?,
        serialize: (T) -> String,
        fetch: suspend () -> Map<Token, T>
    ): Map<Token, T> {
        getCachedTokenMap(providerId, deserialize)?.let { return it }
        val data = fetch()
        saveTokenMap(providerId, data, serialize)
        return data
    }

    fun <T> getCachedTokenMap(
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

    fun getCachedChainIds(providerId: String): List<String>? {
        val oldestTimestamp = chainDao.getOldestTimestamp(providerId) ?: return null
        if (System.currentTimeMillis() - oldestTimestamp > CACHE_LIFETIME_MS) {
            chainDao.deleteByProvider(providerId)
            return null
        }
        return chainDao.getByProvider(providerId).map { it.chainId }
    }

    fun saveChainIds(providerId: String, chainIds: List<String>) {
        val timestamp = System.currentTimeMillis()
        chainDao.deleteByProvider(providerId)
        chainDao.insertAll(chainIds.map { SwapProviderChainRecord(providerId, it, timestamp) })
    }

    fun <T> saveTokenMap(
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
