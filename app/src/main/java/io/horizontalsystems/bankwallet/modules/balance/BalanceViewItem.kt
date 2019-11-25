package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
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
        val currencyValueLocked: CurrencyValue?,
        var updateType: UpdateType?,
        var xExpanded: Boolean
) {
    enum class UpdateType {
        MARKET_INFO,
        CHART_INFO,
        BALANCE,
        STATE,
        EXPANDED
    }

    var xButtonPayEnabled = false
    var xButtonReceiveEnabled = true
    var xImgSyncFailedVisibility = View.INVISIBLE
    var xIconProgressVisibility = View.INVISIBLE
    var xTextProgressText: String? = null
    var xIconProgressValue: Float? = null
    var xTextSyncedUntilText: String? = null
    var xCoinIconVisibility = View.VISIBLE

    val xCoinAmountText = App.numberFormatter.format(coinValue)
    val xCoinAmountAlpha = if (state is AdapterState.Synced) 1f else 0.3f

    var xCoinAmountLockedVisibility = View.GONE
    var xFiatAmountLockedVisibility = View.GONE
    var xCoinAmountLockedText: String? = null
    var xFiatAmountLockedText: String? = null
    var xFiatAmountLockedAlpha: Float? = null

    val xIconDrawableResource = LayoutHelper.getCoinDrawableResource(coin.code)
    val xTextCoinNameText = coin.title

    val xExchangeRateTextColor = ContextCompat.getColor(App.instance, if (marketInfoExpired) R.color.grey_50 else R.color.grey)
    val xExchangeRateText: CharSequence? = exchangeValue?.let { exchangeValue ->
        val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
        when {
            chartInfoState is ChartInfoState.Loaded -> rateString
            else -> App.instance.getString(R.string.Balance_RatePerCoin, rateString, coin.code)
        }
    }

    var xFiatAmountText: String? = null
    var xFiatAmountAlpha: Float? = null

    val xTypeLabelText = coin.type.typeLabel()

    val xButtonsWrapperVisibility = if (xExpanded) View.VISIBLE else View.GONE

    val xFiatAmountVisibility: Int

    val xSyncingStateGroupVisibility: Int
    val xCoinAmountVisibility: Int
    val xCoinTypeLabelVisibility: Int

    init {
        var xSyncing = false

        state.let { adapterState ->
            when (adapterState) {
                is AdapterState.NotReady -> {
                    xSyncing = true
                    xIconProgressVisibility = View.VISIBLE
                    xTextProgressText = App.instance.getString(R.string.Balance_Syncing)
                    xButtonReceiveEnabled = false
                }
                is AdapterState.Syncing -> {
                    xSyncing = true
                    xIconProgressVisibility = View.VISIBLE
                    xIconProgressValue = adapterState.progress.toFloat()
                    xTextSyncedUntilText = adapterState.lastBlockDate?.let {
                        App.instance.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(it, "MMM d.yyyy"))
                    }

                    xTextProgressText = App.instance.getString(R.string.Balance_Syncing_WithProgress, adapterState.progress.toString())
                }
                is AdapterState.Synced -> {
                    if (coinValue.value > BigDecimal.ZERO) {
                        xButtonPayEnabled = true
                    }

                    xCoinIconVisibility = View.VISIBLE
                }
                is AdapterState.NotSynced -> {
                    xImgSyncFailedVisibility = View.VISIBLE
                    xCoinIconVisibility = View.GONE
                }
            }
        }


        if (this.coinValueLocked.value > BigDecimal.ZERO) {
            xCoinAmountLockedVisibility = View.VISIBLE
            xCoinAmountLockedText = App.numberFormatter.format(this.coinValueLocked)

            this.currencyValueLocked?.let {
                xFiatAmountLockedVisibility = View.VISIBLE
                xFiatAmountLockedText = App.numberFormatter.format(it, trimmable = true)
                xFiatAmountLockedAlpha = if (!this.marketInfoExpired && this.state is AdapterState.Synced) 1f else 0.5f
            }
        }

        currencyValue?.let {
            xFiatAmountText = App.numberFormatter.format(it, trimmable = true)
            xFiatAmountAlpha = if (!marketInfoExpired && state is AdapterState.Synced) 1f else 0.5f
        }

        xFiatAmountVisibility = when {
            xSyncing && !xExpanded -> View.GONE
            currencyValue == null -> View.GONE
            currencyValue.value.compareTo(BigDecimal.ZERO) == 0 -> View.GONE
            else -> View.VISIBLE
        }

        xSyncingStateGroupVisibility = if (xSyncing && !xExpanded) View.VISIBLE else View.GONE
        xCoinAmountVisibility = if (xSyncing && !xExpanded) View.INVISIBLE else View.VISIBLE

        xCoinTypeLabelVisibility = when {
            xSyncing && !xExpanded -> View.GONE
            else -> if (xTypeLabelText == null) View.GONE else View.VISIBLE
        }
    }
}

data class BalanceHeaderViewItem(
        val currencyValue: CurrencyValue?,
        val upToDate: Boolean
) {
    val xBalanceText = currencyValue?.let {
        App.numberFormatter.format(it)
    }

    val xBalanceTextColor = ContextCompat.getColor(App.instance, if (upToDate) R.color.yellow_d else R.color.yellow_50)
}

class BalanceViewItemFactory {

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, updateType: BalanceViewItem.UpdateType?, expanded: Boolean): BalanceViewItem {
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
                currencyValueLocked,
                updateType,
                expanded
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
