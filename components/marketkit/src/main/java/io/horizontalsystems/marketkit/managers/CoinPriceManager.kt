package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.storage.CoinPriceStorage

class CoinPriceManager(
    private val storage: CoinPriceStorage
) {
    interface Listener {
        fun didUpdate(coinPriceMap: Map<String, CoinPrice>, currencyCode: String)
    }

    var listener: Listener? = null

    private fun notify(coinPrices: List<CoinPrice>, currencyCode: String) {
        val coinPriceMap = mutableMapOf<String, CoinPrice>()

        coinPrices.forEach { coinPrice ->
            coinPriceMap[coinPrice.coinUid] = coinPrice
        }

        listener?.didUpdate(coinPriceMap, currencyCode)
    }

    fun lastSyncTimeStamp(coinUids: List<String>, currencyCode: String): Long? {
        val coinPrices = storage.coinPricesSortedByTimestamp(coinUids, currencyCode)

        // not all records for coin codes are stored in database - force sync required
        if (coinPrices.size != coinUids.size) {
            return null
        }

        // return date of the most expired stored record
        return coinPrices.firstOrNull()?.timestamp
    }

    fun coinPrice(coinUid: String, currencyCode: String): CoinPrice? {
        return storage.coinPrice(coinUid, currencyCode)
    }

    fun coinPriceMap(coinUids: List<String>, currencyCode: String): Map<String, CoinPrice> {
        val coinPriceMap = mutableMapOf<String, CoinPrice>()

        storage.coinPrices(coinUids, currencyCode).forEach { coinPrice ->
            coinPriceMap[coinPrice.coinUid] = coinPrice
        }

        return coinPriceMap
    }

    fun handleUpdated(coinPrices: List<CoinPrice>, currencyCode: String) {
        storage.save(coinPrices)
        notify(coinPrices, currencyCode)
    }

    fun notifyExpired(coinUids: List<String>, currencyCode: String) {
        val coinPrices = storage.coinPrices(coinUids, currencyCode)
        notify(coinPrices, currencyCode)
    }
}