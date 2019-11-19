package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
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
        val chartInfoState: ChartInfoState,
        val coinValueLocked: CoinValue,
        val currencyValueLocked: CurrencyValue?
)

data class BalanceHeaderViewItem(
        val currencyValue: CurrencyValue?,
        val upToDate: Boolean
)

class BalanceViewItemFactory {

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency): BalanceViewItem {
        val balanceTotal = item.balanceTotal ?: BigDecimal.ZERO
        val balanceLocked = item.balanceLocked ?: BigDecimal.ZERO

        var exchangeValue: CurrencyValue? = null
        var currencyValueTotal: CurrencyValue? = null
        var currencyValueLocked: CurrencyValue? = null

        item.marketInfo?.rate?.let { rate ->
            exchangeValue = CurrencyValue(currency, rate)
            currencyValueTotal = CurrencyValue(currency, rate * balanceTotal)
            currencyValueLocked = CurrencyValue(currency, rate * balanceLocked)
        }

        return BalanceViewItem(
                item.wallet,
                item.wallet.coin,
                CoinValue(item.wallet.coin, balanceTotal),
                exchangeValue,
                item.marketInfo?.diff,
                currencyValueTotal,
                item.state ?: AdapterState.NotReady,
                item.marketInfo?.isExpired() ?: false,
                item.chartInfoState,
                CoinValue(item.wallet.coin, balanceLocked),
                currencyValueLocked
        )
    }

    fun headerViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency): BalanceHeaderViewItem {
        var total = BigDecimal.ZERO
        var upToDate = true

        items.forEach { item ->
            val balanceTotal = item.balanceTotal
            val marketInfo = item.marketInfo

            if (balanceTotal != null && marketInfo != null) {
                total += balanceTotal.multiply(marketInfo.rate)

                upToDate = !marketInfo.isExpired()
            }

            if (item.state == null || item.state != AdapterState.Synced) {
                upToDate = false
            }
        }

        return BalanceHeaderViewItem(CurrencyValue(currency, total), upToDate)
    }

}
