package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal

data class BalanceViewItem(
        val coinValue: CoinValue,
        val exchangeValue: CurrencyValue?,
        val currencyValue: CurrencyValue?,
        val state: AdapterState,
        val rateExpired: Boolean
)

data class BalanceHeaderViewItem(
        val currencyValue: CurrencyValue?,
        val upToDate: Boolean
)

class BalanceViewItemFactory {

    fun createViewItem(item: BalanceModule.BalanceItem, currency: Currency?): BalanceViewItem {
        var exchangeValue: CurrencyValue? = null
        var currencyValue: CurrencyValue? = null

        item.rate?.let { rate ->
            currency?.let {
                exchangeValue = CurrencyValue(it, rate.value)
                currencyValue = CurrencyValue(it, rate.value * item.balance)
            }
        }

        return BalanceViewItem(
                CoinValue(item.coinCode, item.balance),
                exchangeValue,
                currencyValue,
                item.state,
                item.rate?.expired ?: false
        )
    }

    fun createHeaderViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency?): BalanceHeaderViewItem {
        var sum = BigDecimal.ZERO
        items.forEach {
            sum = sum.plus(it.rate?.value?.times(it.balance) ?: BigDecimal.ZERO)
        }
        val currencyValue = currency?.let {
            CurrencyValue(it, sum)
        }

        val upToDate = items.none { it.state != AdapterState.Synced || it.rate == null || it.rate?.expired == true }

        return BalanceHeaderViewItem(currencyValue, upToDate)
    }

}
