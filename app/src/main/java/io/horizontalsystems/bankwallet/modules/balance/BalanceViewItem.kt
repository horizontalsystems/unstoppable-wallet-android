package io.horizontalsystems.bankwallet.modules.balance

import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBlockedChartCoins
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal
import java.math.RoundingMode

data class BalanceViewItem(
        val wallet: Wallet,
        val coinCode: String,
        val coinTitle: String,
        val coinType: String?,
        val coinValue: DeemedValue,
        val exchangeValue: DeemedValue,
        val diff: RateDiff,
        val fiatValue: DeemedValue,
        val coinValueLocked: DeemedValue,
        val fiatValueLocked: DeemedValue,
        val updateType: UpdateType?,
        val expanded: Boolean,
        val sendEnabled: Boolean = false,
        val receiveEnabled: Boolean = false,
        val syncingData: SyncingData,
        val failedIconVisible: Boolean,
        val coinIconVisible: Boolean,
        val coinTypeLabelVisible: Boolean,
        val blockChart: Boolean
) {
    enum class UpdateType {
        MARKET_INFO,
        BALANCE,
        STATE,
        EXPANDED
    }
}

data class RateDiff(
        val deemedValue:DeemedValue,
        val positive: Boolean
)

data class BalanceHeaderViewItem(val currencyValue: CurrencyValue?, val upToDate: Boolean) {

    val xBalanceText = currencyValue?.let {
        App.numberFormatter.format(it)
    }

    val xBalanceTextColor = ContextCompat.getColor(App.instance, if (upToDate) R.color.yellow_d else R.color.yellow_50)
}

class DeemedValue(val text: String?, val dimmed: Boolean = false, val visible: Boolean = true)
class SyncingData(val progress: Int?, val until: String?, val syncingTextVisible: Boolean = true)

class BalanceViewItemFactory(private val blockedChartCoins: IBlockedChartCoins) {

    private val diffScale = 2

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
            balance?.let {
                App.numberFormatter.format(CurrencyValue(currency, it * rate), trimmable = true)
            }
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(currency: Currency, marketInfo: MarketInfo?): DeemedValue {
        var dimmed = false
        val value = marketInfo?.let {
            dimmed = marketInfo.isExpired()
            App.numberFormatter.formatForRates(CurrencyValue(currency, marketInfo.rate), trimmable = true)
        }

        return DeemedValue(value, dimmed = dimmed)
    }

    private fun syncingData(state: AdapterState?, expanded: Boolean): SyncingData {

        if (state !is AdapterState.Syncing) {
            return SyncingData(null, null, false)
        }

        val dateFormatted = state.lastBlockDate?.let { until ->
            DateHelper.formatDate(until, "MMM d, yyyy")
        }

        return SyncingData(state.progress, dateFormatted, !expanded)
    }

    private fun coinTypeLabelVisible(coinType: CoinType): Boolean {
        return coinType.typeLabel() != null
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, updateType: BalanceViewItem.UpdateType?, expanded: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val marketInfo = item.marketInfo

        val balanceTotalVisibility = item.balanceTotal != null && (state !is AdapterState.Syncing || expanded)
        val balanceLockedVisibility = item.balanceLocked != null

        val rateDiff = getRateDiff(item)

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
                diff = rateDiff,
                updateType = updateType,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                receiveEnabled = state != null,
                syncingData = syncingData(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                coinTypeLabelVisible = coinTypeLabelVisible(coin.type),
                blockChart = blockedChartCoins.blockedCoins.contains(coin.code)
        )
    }

    private fun getRateDiff(item: BalanceModule.BalanceItem): RateDiff {
        val scaledValue = item.marketInfo?.diff?.setScale(diffScale, RoundingMode.HALF_EVEN)?.stripTrailingZeros()
        val isPositive = (scaledValue ?: BigDecimal.ZERO) >= BigDecimal.ZERO
        val rateDiffText = scaledValue?.let { App.numberFormatter.format(scaledValue.abs(), diffScale) + "%" }
        val dimmed = item.marketInfo?.isExpired() ?: true
        return RateDiff(DeemedValue(rateDiffText, dimmed, true), isPositive)
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
