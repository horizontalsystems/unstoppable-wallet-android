package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.xrateskit.entities.ChartInfo
import java.math.BigDecimal

data class BalanceViewItem(
        val wallet: Wallet,
        val coin: Coin,
        val coinValue: CoinValue,
        val exchangeValue: CurrencyValue?,
        val diff: BigDecimal?,
        val currencyValue: CurrencyValue?,
        val state: AdapterState,
        val marketInfoExpired: Boolean,
        val chartInfo: ChartInfo?
)

data class BalanceHeaderViewItem(
        val currencyValue: CurrencyValue?,
        val upToDate: Boolean
)

class BalanceViewItemFactory {

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency): BalanceViewItem {
        var exchangeValue: CurrencyValue? = null
        var currencyValue: CurrencyValue? = null

        item.marketInfo?.rate?.let { rate ->
            exchangeValue = CurrencyValue(currency, rate)
            item.balance?.let {
                currencyValue = CurrencyValue(currency, rate * it)
            }
        }

        return BalanceViewItem(
                item.wallet,
                item.wallet.coin,
                CoinValue(item.wallet.coin, item.balance ?: BigDecimal.ZERO),
                exchangeValue,
                item.marketInfo?.diff,
                currencyValue,
                item.state ?: AdapterState.NotReady,
                item.marketInfo?.isExpired() ?: false,
                item.chartInfo
        )
    }

    fun headerViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency): BalanceHeaderViewItem {
        var total = BigDecimal.ZERO
        var upToDate = true

        items.forEach { item ->
            val balance = item.balance
            val marketInfo = item.marketInfo

            if (balance != null && marketInfo != null) {
                total += balance.multiply(marketInfo.rate)

                upToDate = !marketInfo.isExpired()
            }

            if (item.state == null || item.state != AdapterState.Synced) {
                upToDate = false
            }
        }

        return BalanceHeaderViewItem(CurrencyValue(currency, total), upToDate)
    }

}
