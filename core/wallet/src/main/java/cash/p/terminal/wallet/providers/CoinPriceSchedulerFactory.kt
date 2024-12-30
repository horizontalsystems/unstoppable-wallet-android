package cash.p.terminal.wallet.providers

import cash.p.terminal.wallet.Scheduler
import cash.p.terminal.wallet.managers.CoinPriceManager
import cash.p.terminal.wallet.managers.ICoinPriceCoinUidDataSource

class CoinPriceSchedulerFactory(
    private val manager: CoinPriceManager,
    private val provider: HsProvider
) {
    fun scheduler(currencyCode: String, coinUidDataSource: ICoinPriceCoinUidDataSource): Scheduler {
        val schedulerProvider = CoinPriceSchedulerProvider(currencyCode, manager, provider)
        schedulerProvider.dataSource = coinUidDataSource
        return Scheduler(schedulerProvider)
    }
}
