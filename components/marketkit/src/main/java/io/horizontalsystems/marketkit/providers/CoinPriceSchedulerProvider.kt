package io.horizontalsystems.marketkit.providers

import io.horizontalsystems.marketkit.managers.CoinPriceManager
import io.horizontalsystems.marketkit.managers.ICoinPriceCoinUidDataSource
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Single

interface ISchedulerProvider {
    val id: String
    val lastSyncTimestamp: Long?
    val expirationInterval: Long
    val syncSingle: Single<Unit>

    fun notifyExpired()
}

class CoinPriceSchedulerProvider(
    private val currencyCode: String,
    private val manager: CoinPriceManager,
    private val provider: HsProvider
) : ISchedulerProvider {
    var dataSource: ICoinPriceCoinUidDataSource? = null

    override val id = "CoinPriceProvider"

    override val lastSyncTimestamp: Long?
        get() = manager.lastSyncTimeStamp(coinUids, currencyCode)

    override val expirationInterval: Long
        get() = CoinPrice.expirationInterval

    override val syncSingle: Single<Unit>
        get() = provider.getCoinPrices(coinUids, currencyCode)
            .doOnSuccess {
                handle(it)
            }.map {}

    private val coinUids: List<String>
        get() = dataSource?.coinUids(currencyCode) ?: listOf()

    override fun notifyExpired() {
        manager.notifyExpired(coinUids, currencyCode)
    }

    private fun handle(updatedCoinPrices: List<CoinPrice>) {
        manager.handleUpdated(updatedCoinPrices, currencyCode)
    }
}
