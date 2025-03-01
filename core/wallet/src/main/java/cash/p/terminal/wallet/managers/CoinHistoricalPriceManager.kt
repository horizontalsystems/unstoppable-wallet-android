package cash.p.terminal.wallet.managers

import android.util.Log
import cash.p.terminal.wallet.ProviderError
import cash.p.terminal.wallet.models.CoinHistoricalPrice
import cash.p.terminal.wallet.providers.HsProvider
import cash.p.terminal.wallet.storage.CoinHistoricalPriceStorage
import java.math.BigDecimal
import kotlin.math.abs

class CoinHistoricalPriceManager(
    private val storage: CoinHistoricalPriceStorage,
    private val hsProvider: HsProvider,
) {

    suspend fun coinHistoricalPriceSingle(
        coinUid: String,
        currencyCode: String,
        timestamp: Long
    ): BigDecimal {

        storage.coinPrice(coinUid, currencyCode, timestamp)?.let {
            return it.value
        }

        val response = hsProvider.historicalCoinPriceSingle(coinUid, currencyCode, timestamp)
        if (abs(timestamp - response.timestamp) < 24 * 60 * 60) {
            val coinHistoricalPrice =
                CoinHistoricalPrice(coinUid, currencyCode, response.price, timestamp)
            Log.d("CoinHistoricalPriceManager", "Saving coinHistoricalPrice: $coinHistoricalPrice")
            storage.save(coinHistoricalPrice)
            return response.price
        }
        throw ProviderError.ReturnedTimestampIsVeryInaccurate()
    }

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long): BigDecimal? {
        return storage.coinPrice(coinUid, currencyCode, timestamp)?.value
    }

}
