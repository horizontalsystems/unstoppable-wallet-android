package io.horizontalsystems.bankwallet.modules.balance

import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

data class BalanceViewItem(
        val wallet: Wallet,
        val coinCode: String,
        val coinTitle: String,
        val coinType: String?,
        val coinValue: DeemedValue,
        val exchangeValue: DeemedValue,
        val diff: BigDecimal?,
        val fiatValue: DeemedValue,
        val marketInfoExpired: Boolean,
        val chartInfoState: ChartInfoState,
        val chartData: ChartData,
        val coinValueLocked: DeemedValue,
        val fiatValueLocked: DeemedValue,
        var updateType: UpdateType?,
        var xExpanded: Boolean,
        val xButtonSendEnabled: Boolean = false,
        val xButtonReceiveEnabled: Boolean = false,
        val xSyncingData: SyncingData,
        val xImgSyncFailedVisible: Boolean,
        val xCoinIconVisible: Boolean
) {

    val xRateDiffVisible = !xExpanded
    val xCoinTypeLabelVisible = coinType != null && coinValue.visible && coinValue.text != null

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

class DeemedValue(val text: String?, val dimmed: Boolean = false, val visible: Boolean = true)
class SyncingData(val progress: Int?, val until: String?, val syncingTextVisible: Boolean = true)
class ChartData(
        val wrapperVisible: Boolean,
        val loading: Boolean,
        val failed: Boolean,
        val loaded: Boolean,
        val points: List<ChartPoint>,
        val startTimestamp: Long,
        val endTimestamp: Long
)

class BalanceViewItemFactory {

    private fun chartData(chartInfoState: ChartInfoState, expanded: Boolean): ChartData {
        val loading = chartInfoState is ChartInfoState.Loading
        val failed = chartInfoState is ChartInfoState.Failed
        val loaded = chartInfoState is ChartInfoState.Loaded
        val chartInfo = (chartInfoState as? ChartInfoState.Loaded)?.chartInfo

        val points = chartInfo?.points?.map { ChartPoint(it.value.toFloat(), it.timestamp) } ?: listOf()
        val startTimestamp = chartInfo?.startTimestamp ?: 0
        val endTimestamp = chartInfo?.endTimestamp ?: 0
        val wrapperVisible = !expanded

        return ChartData(wrapperVisible, loading, failed, loaded, points, startTimestamp, endTimestamp)
    }

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
            when {
                balance != null && balance > BigDecimal.ZERO -> {
                    App.numberFormatter.format(CurrencyValue(currency, balance * rate), trimmable = true)
                }
                else -> null
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

    private fun syncingData(state: AdapterState?, expanded: Boolean): SyncingData {

        if (state !is AdapterState.Syncing) {
            return SyncingData(null, null, false)
        }

        val dateFormatted = state.lastBlockDate?.let { until ->
            DateHelper.formatDate(until, "MMM d.yyyy")
        }

        return SyncingData(state.progress, dateFormatted, !expanded)
    }

    private fun payButtonPayEnabled(state: AdapterState?, balance: BigDecimal?): Boolean {
        return state is AdapterState.Synced && balance != null && balance > BigDecimal.ZERO
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, updateType: BalanceViewItem.UpdateType?, expanded: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val marketInfo = item.marketInfo

        val balanceTotalVisibility = item.balanceTotal != null && (state !is AdapterState.Syncing || expanded)
        val balanceLockedVisibility = item.balanceLocked != null

        return BalanceViewItem(
                wallet = item.wallet,
                coinCode = coin.code,
                coinTitle = coin.title,
                coinType = coin.type.typeLabel(),
                coinValue = coinValue(state, item.balanceTotal, coin, balanceTotalVisibility),
                coinValueLocked = coinValue(state, item.balanceLocked, coin, balanceLockedVisibility),
                fiatValue = currencyValue(state, item.balanceTotal, currency, marketInfo, balanceTotalVisibility),
                fiatValueLocked = currencyValue(state, item.balanceLocked, currency, marketInfo, balanceLockedVisibility),
                exchangeValue = rateValue(currency, marketInfo),
                diff = item.marketInfo?.diff,
                marketInfoExpired = item.marketInfo?.isExpired() ?: false,
                chartInfoState = item.chartInfoState,
                chartData = chartData(item.chartInfoState, expanded),
                updateType = updateType,
                xExpanded = expanded,
                xButtonSendEnabled = payButtonPayEnabled(state, item.balanceTotal),
                xButtonReceiveEnabled = state != null,
                xSyncingData = syncingData(state, expanded),
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
