package cash.p.terminal.wallet.managers

import cash.p.terminal.wallet.ProviderError
import cash.p.terminal.wallet.models.CoinHistoricalPrice
import cash.p.terminal.wallet.providers.HsProvider
import cash.p.terminal.wallet.storage.CoinHistoricalPriceStorage
import io.reactivex.Single
import java.math.BigDecimal
import kotlin.math.abs

class CoinHistoricalPriceManager(
    private val storage: CoinHistoricalPriceStorage,
    private val hsProvider: HsProvider,
) {

    fun coinHistoricalPriceSingle(
        coinUid: String,
        currencyCode: String,
        timestamp: Long
    ): Single<BigDecimal> {

        storage.coinPrice(coinUid, currencyCode, timestamp)?.let {
            return Single.just(it.value)
        }

        return hsProvider.historicalCoinPriceSingle(coinUid, currencyCode, timestamp)
            .flatMap { response ->
                if (abs(timestamp - response.timestamp) < 24 * 60 * 60) {
                    val coinHistoricalPrice = CoinHistoricalPrice(coinUid, currencyCode, response.price, timestamp)
                    storage.save(coinHistoricalPrice)
                    Single.just(response.price)
                } else {
                    Single.error(ProviderError.ReturnedTimestampIsVeryInaccurate())
                }
            }
    }

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long): BigDecimal? {
        return storage.coinPrice(coinUid, currencyCode, timestamp)?.value
    }

}
