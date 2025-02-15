package cash.p.terminal.wallet.providers

import cash.p.terminal.wallet.managers.CoinPriceManager
import cash.p.terminal.wallet.managers.ICoinPriceCoinUidDataSource
import cash.p.terminal.wallet.models.CoinPrice
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val id = "CoinPriceProvider"

    override val lastSyncTimestamp: Long?
        get() = manager.lastSyncTimeStamp(allCoinUids, currencyCode)

    override val expirationInterval: Long
        get() = CoinPrice.expirationInterval

    override val syncSingle: Single<Unit>
        get() {
            val (coinUids, walletUids) = combinedCoinUids
            return Single.just(coroutineScope.launch {
                runCatching {
                    handle(
                        provider.getCoinPrices(
                            coinUids,
                            walletUids,
                            currencyCode
                        )
                    )
                }
            }).map { }
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
