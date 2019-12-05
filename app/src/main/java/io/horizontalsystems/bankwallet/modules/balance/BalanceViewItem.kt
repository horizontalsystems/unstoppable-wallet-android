package io.horizontalsystems.bankwallet.modules.balance

import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

data class BalanceViewItem(
        val wallet: Wallet,
        val coinIconCode: String,
        val coinTitle: String,
        val coinLabel: String?,
        val coinValue: DeemedValue,
        val exchangeValue: DeemedValue,
        val diff: BigDecimal?,
        val currencyValue: DeemedValue,
        val marketInfoExpired: Boolean,
        val chartInfoState: ChartInfoState,
        val coinValueLocked: DeemedValue,
        val currencyValueLocked: DeemedValue,
        var updateType: UpdateType?,
        var xExpanded: Boolean,
        val xButtonPayEnabled: Boolean = false,
        val xButtonReceiveEnabled: Boolean = false,
        val xIconProgress: Int?,
        val xTextProgress: ProgressInfo,
        val xImgSyncFailedVisible: Boolean,
        val xCoinIconVisible: Boolean
) {

    val xChartWrapperVisible = !xExpanded
    val xRateDiffVisible = !xExpanded
    val xChartVisible = chartInfoState is ChartInfoState.Loaded
    val xChartLoadingVisible = chartInfoState is ChartInfoState.Loading
    val xChartErrorVisible = chartInfoState is ChartInfoState.Failed
    val xChartInfo = (chartInfoState as? ChartInfoState.Loaded)?.chartInfo

    val xCoinTypeLabelVisible = coinLabel != null && coinValue.visible

    enum class UpdateType {
        MARKET_INFO,
        CHART_INFO,
        BALANCE,
        STATE,
        EXPANDED
    }
}

data class BalanceHeaderViewItem(val currencyValue: CurrencyValue?, val upToDate: Boolean) {

    val xBalanceText = currencyValue?.let {
        App.numberFormatter.format(it)
    }

    val xBalanceTextColor = ContextCompat.getColor(App.instance, if (upToDate) R.color.yellow_d else R.color.yellow_50)
}

class DeemedValue(val value: String?, val dimmed: Boolean = false, val visible: Boolean = true)
class ProgressInfo(val value: Int?, val until: String?, val visible: Boolean = true)

class BalanceViewItemFactory {

    private fun coinValue(state: AdapterState?, balance: BigDecimal?, coin: Coin, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced
        val value = balance?.let {
            App.numberFormatter.format(CoinValue(coin, balance))
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun currencyValue(state: AdapterState?, balance: BigDecimal?, currency: Currency, marketInfo: MarketInfo?, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced || marketInfo?.isExpired() ?: false
        val value = marketInfo?.rate?.let { rate ->
            when (balance) {
                null, BigDecimal.ZERO -> null
                else -> App.numberFormatter.format(CurrencyValue(currency, balance * rate), trimmable = true)
            }
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(currency: Currency, marketInfo: MarketInfo?): DeemedValue {
        var dimmed = false
        val value = marketInfo?.let {
            dimmed = marketInfo.isExpired()
            App.numberFormatter.format(CurrencyValue(currency, marketInfo.rate), trimmable = true, canUseLessSymbol = false)
        }

        return DeemedValue(value, dimmed = dimmed)
    }

    private fun iconSyncing(state: AdapterState?): Int? {
        return when (state) {
            is AdapterState.Syncing -> state.progress
            else -> null
        }
    }

    private fun textSyncing(state: AdapterState?, visible: Boolean): ProgressInfo {
        if (state !is AdapterState.Syncing) {
            return ProgressInfo(null, null, false)
        }

        if (state.lastBlockDate == null) {
            return ProgressInfo(null, null, visible)
        }

        val dateFormatted = DateHelper.formatDate(state.lastBlockDate, "MMM d.yyyy")

        return ProgressInfo(state.progress, dateFormatted, visible)
    }

    private fun payButtonPayEnabled(state: AdapterState?, balance: BigDecimal?): Boolean {
        return state is AdapterState.Synced && balance != null && balance > BigDecimal.ZERO
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, updateType: BalanceViewItem.UpdateType?, expanded: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val marketInfo = item.marketInfo

        val balanceTotalVisibility = state !is AdapterState.Syncing || expanded
        val balanceLockedVisibility = item.balanceLocked != null

        return BalanceViewItem(
                wallet = item.wallet,
                coinIconCode = coin.code,
                coinTitle = coin.title,
                coinLabel = coin.type.typeLabel(),
                coinValue = coinValue(state, item.balanceTotal, coin, balanceTotalVisibility),
                coinValueLocked = coinValue(state, item.balanceLocked, coin, balanceLockedVisibility),
                currencyValue = currencyValue(state, item.balanceTotal, currency, marketInfo, balanceTotalVisibility),
                currencyValueLocked = currencyValue(state, item.balanceLocked, currency, marketInfo, balanceLockedVisibility),
                exchangeValue = rateValue(currency, marketInfo),
                diff = item.marketInfo?.diff,
                marketInfoExpired = item.marketInfo?.isExpired() ?: false,
                chartInfoState = item.chartInfoState,
                updateType = updateType,
                xExpanded = expanded,
                xButtonPayEnabled = payButtonPayEnabled(state, item.balanceTotal),
                xButtonReceiveEnabled = state != null,
                xIconProgress = iconSyncing(state),
                xTextProgress = textSyncing(state, !expanded),
                xImgSyncFailedVisible = state is AdapterState.NotSynced,
                xCoinIconVisible = state !is AdapterState.NotSynced
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
