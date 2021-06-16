package io.horizontalsystems.bankwallet.modules.balance

import android.content.Context
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.blockchainType
import io.horizontalsystems.bankwallet.entities.swappable
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.LatestRate
import java.math.BigDecimal
import java.util.*

data class BalanceViewItem(
    val wallet: Wallet,
    val coinCode: String,
    val coinTitle: String,
    val coinType: CoinType,
    val coinValue: DeemedValue,
    val exchangeValue: DeemedValue,
    val diff: BigDecimal?,
    val fiatValue: DeemedValue,
    val coinValueLocked: DeemedValue,
    val fiatValueLocked: DeemedValue,
    val expanded: Boolean,
    val sendEnabled: Boolean = false,
    val receiveEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: DeemedValue,
    val syncedUntilTextValue: DeemedValue,
    val failedIconVisible: Boolean,
    val coinIconVisible: Boolean,
    val coinTypeLabel: String?,
    val swapVisible: Boolean,
    val swapEnabled: Boolean = false,
    val mainNet: Boolean,
    val errorMessage: String?
)

data class BalanceHeaderViewItem(val xBalanceText: String, val upToDate: Boolean) {

    fun getBalanceTextColor(context: Context): Int {
        val color = if (upToDate) R.color.jacob else R.color.yellow_50
        return context.getColor(color)
    }

}

data class DeemedValue(val text: String?, val dimmed: Boolean = false, val visible: Boolean = true)
data class SyncingProgress(val progress: Int?, val dimmed: Boolean = false)

class BalanceViewItemFactory {

    private fun coinValue(state: AdapterState?, balance: BigDecimal?, coin: Coin, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced
        val value = balance?.let {
            val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(it)
            App.numberFormatter.format(balance, 0, significantDecimal)
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun currencyValue(state: AdapterState?, balance: BigDecimal?, currency: Currency, latestRate: LatestRate?, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced || latestRate?.isExpired() ?: false
        val value = latestRate?.rate?.let { rate ->
            balance?.let {
                App.numberFormatter.formatFiat(it * rate, currency.symbol, 0, 2)
            }
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(currency: Currency, latestRate: LatestRate?, showSyncing: Boolean): DeemedValue {
        var dimmed = false
        val value = latestRate?.let {
            dimmed = latestRate.isExpired()
            App.numberFormatter.formatFiat(latestRate.rate, currency.symbol, 2, 4)
        }

        return DeemedValue(value, dimmed = dimmed, visible = !showSyncing)
    }

    private fun getSyncingProgress(state: AdapterState?): SyncingProgress {
        return when (state) {
            is AdapterState.Syncing -> SyncingProgress(state.progress, false)
            is AdapterState.SearchingTxs -> SyncingProgress(10, true)
            else -> SyncingProgress(null, false)
        }
    }

    private fun getSyncingText(state: AdapterState?, expanded: Boolean): DeemedValue {
        if (state == null || !expanded) {
            return DeemedValue(null, false, false)
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.lastBlockDate != null) {
                    Translator.getString(R.string.Balance_Syncing_WithProgress, state.progress.toString())
                } else {
                    Translator.getString(R.string.Balance_Syncing)
                }
            }
            is AdapterState.SearchingTxs -> Translator.getString(R.string.Balance_SearchingTransactions)
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun getSyncedUntilText(state: AdapterState?, expanded: Boolean): DeemedValue {
        if (state == null || !expanded) {
            return DeemedValue(null, false, false)
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.lastBlockDate != null) {
                    Translator.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(state.lastBlockDate, "MMM d, yyyy"))
                } else {
                    null
                }
            }
            is AdapterState.SearchingTxs -> {
                if (state.count > 0) {
                    Translator.getString(R.string.Balance_FoundTx, state.count.toString())
                } else {
                    null
                }
            }
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun coinTypeLabel(wallet: Wallet) = when (wallet.coin.type) {
        CoinType.Bitcoin,
        CoinType.Litecoin -> {
            wallet.configuredCoin.settings.derivation?.value?.toUpperCase(Locale.ENGLISH)
        }
        CoinType.BitcoinCash -> {
            wallet.configuredCoin.settings.bitcoinCashCoinType?.value?.toUpperCase(Locale.ENGLISH)
        }
        else -> {
            wallet.coin.type.blockchainType
        }
    }

    private fun lockedCoinValue(state: AdapterState?, balance: BigDecimal?, coinCode: String, hideBalance: Boolean): DeemedValue {
        val visible = balance != null
        val deemed = state !is AdapterState.Synced

        val value =
                if (hideBalance)
                    Translator.getString(R.string.Balance_Hidden)
                else
                    balance?.let {
                        val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(it)
                        App.numberFormatter.formatCoin(balance, coinCode, 0, significantDecimal)
                    }

        return DeemedValue(value, deemed, visible)
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, expanded: Boolean, hideBalance: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.latestRate

        val showSyncing = expanded && (state is AdapterState.Syncing || state is AdapterState.SearchingTxs)
        val balanceTotalVisibility = !hideBalance && item.balanceTotal != null && !showSyncing
        val fiatLockedVisibility = !hideBalance && item.balanceLocked != null

        return BalanceViewItem(
                wallet = item.wallet,
                coinCode = coin.code,
                coinTitle = coin.title,
                coinType = coin.type,
                coinValue = coinValue(state, item.balanceTotal, coin, balanceTotalVisibility),
                fiatValue = currencyValue(state, item.balanceTotal, currency, latestRate, balanceTotalVisibility),
                coinValueLocked = lockedCoinValue(state, item.balanceLocked, coin.code, hideBalance),
                fiatValueLocked = currencyValue(state, item.balanceLocked, currency, latestRate, fiatLockedVisibility),
                exchangeValue = rateValue(currency, latestRate, showSyncing),
                diff = item.latestRate?.rateDiff24h,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                receiveEnabled = state != null,
                syncingProgress = getSyncingProgress(state),
                syncingTextValue = getSyncingText(state, expanded),
                syncedUntilTextValue = getSyncedUntilText(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                coinTypeLabel = coinTypeLabel(wallet),
                swapVisible = item.wallet.coin.type.swappable,
                swapEnabled = state is AdapterState.Synced,
                mainNet = item.mainNet,
                errorMessage = (state as? AdapterState.NotSynced)?.error?.message
        )
    }

    fun headerViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency, balanceHidden: Boolean): BalanceHeaderViewItem = when {
        balanceHidden -> BalanceHeaderViewItem("*****", true)
        else -> {
            var total = BigDecimal.ZERO
            var upToDate = true

            items.forEach { item ->
                val balanceTotal = item.balanceTotal
                val marketInfo = item.latestRate

                if (balanceTotal != null && marketInfo != null) {
                    total += balanceTotal.multiply(marketInfo.rate)

                    upToDate = !marketInfo.isExpired()
                }

                if (item.state == null || item.state != AdapterState.Synced) {
                    upToDate = false
                }
            }

            val currencyValue = CurrencyValue(currency, total)
            val balanceText = currencyValue.let {
                App.numberFormatter.formatFiat(it.value, it.currency.symbol, 2, 2)
            }

            BalanceHeaderViewItem(balanceText, upToDate)
        }
    }

}
