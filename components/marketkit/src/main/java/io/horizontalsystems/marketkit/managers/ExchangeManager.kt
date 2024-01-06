package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.Exchange
import io.horizontalsystems.marketkit.models.VerifiedExchange
import io.horizontalsystems.marketkit.storage.ExchangeStorage

class ExchangeManager(private val storage: ExchangeStorage) {

    fun imageUrlsMap(ids: List<String>): Map<String, String> {
        val exchanges = storage.exchanges(ids)
        val imageUrls = mutableMapOf<String, String>()
        exchanges.forEach {
            imageUrls[it.id] = it.imageUrl
        }
        return imageUrls
    }

    fun handleFetched(exchanges: List<Exchange>) {
        storage.update(exchanges)
    }

    fun verifiedExchangeUids(): List<String> {
        return storage.verifiedExchanges().map { it.uid }
    }

    fun handleFetchedVerified(exchanges: List<VerifiedExchange>) {
        storage.updateVerifiedExchanges(exchanges)
    }

}
