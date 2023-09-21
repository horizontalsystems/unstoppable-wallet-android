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
        get() = manager.lastSyncTimeStamp(allCoinUids, currencyCode)

    override val expirationInterval: Long
        get() = CoinPrice.expirationInterval

    override val syncSingle: Single<Unit>
        get() {
            val (coinUids, walletUids) = combinedCoinUids
            return provider.getCoinPrices(coinUids, walletUids, currencyCode)
                .doOnSuccess {
                    handle(it)
                }.map {}
        }

    private val allCoinUids: List<String>
        get() = dataSource?.allCoinUids(currencyCode) ?: listOf()

    private val combinedCoinUids: Pair<List<String>, List<String>>
        get() = dataSource?.combinedCoinUids(currencyCode) ?: Pair(listOf(), listOf())

    override fun notifyExpired() {
        manager.notifyExpired(allCoinUids, currencyCode)
    }

    private fun handle(updatedCoinPrices: List<CoinPrice>) {
        manager.handleUpdated(updatedCoinPrices, currencyCode)
    }
}
