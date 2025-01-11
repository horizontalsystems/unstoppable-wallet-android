package cash.p.terminal.wallet.storage

import cash.p.terminal.wallet.models.CoinHistoricalPrice

class CoinHistoricalPriceStorage(marketDatabase: MarketDatabase) {

    private val coinHistoricalPriceDao = marketDatabase.coinHistoricalPriceDao()

    fun coinPrice(coinUid: String, currencyCode: String, timestamp: Long): CoinHistoricalPrice? {
        return coinHistoricalPriceDao.getCoinHistoricalPrice(coinUid, currencyCode, timestamp)
    }

    fun save(coinHistoricalPrice: CoinHistoricalPrice) {
        coinHistoricalPriceDao.insert(coinHistoricalPrice)
    }
}
