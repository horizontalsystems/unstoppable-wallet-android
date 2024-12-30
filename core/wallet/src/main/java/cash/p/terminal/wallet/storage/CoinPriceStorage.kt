package cash.p.terminal.wallet.storage

import cash.p.terminal.wallet.models.CoinPrice

class CoinPriceStorage(marketDatabase: MarketDatabase) {
    private val coinPriceDao = marketDatabase.coinPriceDao()

    fun coinPrice(coinUid: String, currencyCode: String): CoinPrice? {
        return coinPriceDao.getCoinPrice(coinUid, currencyCode)
    }

    fun coinPrices(coinUids: List<String>, currencyCode: String): List<CoinPrice> {
        return coinPriceDao.getCoinPrices(coinUids, currencyCode)
    }

    fun coinPricesSortedByTimestamp(coinUids: List<String>, currencyCode: String): List<CoinPrice> {
        return coinPriceDao.getCoinPricesSortedByTimestamp(coinUids, currencyCode)
    }

    fun save(coinPrices: List<CoinPrice>) {
        coinPriceDao.insert(coinPrices)
    }
}
