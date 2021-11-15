package io.horizontalsystems.bankwallet.modules.balance

import android.content.Context
import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.swappable
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigDecimal

@Immutable
data class BalanceViewItem(
    val wallet: Wallet,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
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
    val badge: String?,
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

    private fun coinValue(state: AdapterState?, balance: BigDecimal?, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced
        val value = balance?.let {
            val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(it)
            App.numberFormatter.format(balance, 0, significantDecimal)
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun currencyValue(state: AdapterState?, balance: BigDecimal, currency: Currency, coinPrice: CoinPrice?, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced || coinPrice?.expired ?: false
        val value = coinPrice?.value?.let { rate ->
            App.numberFormatter.formatFiat(balance * rate, currency.symbol, 0, 2)
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(currency: Currency, coinPrice: CoinPrice?, showSyncing: Boolean): DeemedValue {
        var dimmed = false
        val value = coinPrice?.let {
            dimmed = coinPrice.expired
            val significantDecimal = App.numberFormatter.getSignificantDecimalFiat(coinPrice.value)
            App.numberFormatter.formatFiat(coinPrice.value, currency.symbol, 2, significantDecimal)
        }

        return DeemedValue(value, dimmed = dimmed, visible = !showSyncing)
    }

    private fun getSyncingProgress(state: AdapterState?, coinType: CoinType): SyncingProgress {
        return when (state) {
            is AdapterState.Syncing -> SyncingProgress(state.progress ?: getDefaultSyncingProgress(coinType), false)
            is AdapterState.SearchingTxs -> SyncingProgress(10, true)
            else -> SyncingProgress(null, false)
        }
    }

    private fun getDefaultSyncingProgress(coinType: CoinType): Int {
        return when (coinType) {
            CoinType.Bitcoin, CoinType.Litecoin, CoinType.BitcoinCash, CoinType.Dash, CoinType.Zcash -> 10
            CoinType.Ethereum, CoinType.BinanceSmartChain, is CoinType.Erc20, is CoinType.Bep2, is CoinType.Bep20 -> 50
            is CoinType.ArbitrumOne,
            is CoinType.Avalanche,
            is CoinType.Fantom,
            is CoinType.HarmonyShard0,
            is CoinType.HuobiToken,
            is CoinType.Iotex,
            is CoinType.Moonriver,
            is CoinType.OkexChain,
            is CoinType.PolygonPos,
            is CoinType.Solana,
            is CoinType.Sora,
            is CoinType.Tomochain,
            is CoinType.Xdai,
            is CoinType.Unsupported -> 0
        }
    }

    private fun getSyncingText(state: AdapterState?, expanded: Boolean): DeemedValue {
        if (state == null || !expanded) {
            return DeemedValue(null, false, false)
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.progress != null) {
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

    private fun lockedCoinValue(state: AdapterState?, balance: BigDecimal, coinCode: String, hideBalance: Boolean): DeemedValue {
        val visible = balance > BigDecimal.ZERO
        val deemed = state !is AdapterState.Synced

        val value = if (hideBalance) {
            Translator.getString(R.string.Balance_Hidden)
        } else {
            val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(balance)
            App.numberFormatter.formatCoin(balance, coinCode, 0, significantDecimal)
        }

        return DeemedValue(value, deemed, visible)
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, expanded: Boolean, hideBalance: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.coinPrice

        val showSyncing = expanded && (state is AdapterState.Syncing || state is AdapterState.SearchingTxs)
        val balanceTotalVisibility = !hideBalance && !showSyncing
        val fiatLockedVisibility = !hideBalance && item.balanceData.locked > BigDecimal.ZERO

        return BalanceViewItem(
                wallet = item.wallet,
                coinCode = coin.code,
                coinTitle = coin.name,
                coinIconUrl = coin.iconUrl,
                coinIconPlaceholder = wallet.coinType.iconPlaceholder,
                coinValue = coinValue(state, item.balanceData.total, balanceTotalVisibility),
                fiatValue = currencyValue(state, item.balanceData.total, currency, latestRate, balanceTotalVisibility),
                coinValueLocked = lockedCoinValue(state, item.balanceData.locked, coin.code, hideBalance),
                fiatValueLocked = currencyValue(state, item.balanceData.locked, currency, latestRate, fiatLockedVisibility),
                exchangeValue = rateValue(currency, latestRate, showSyncing),
                diff = item.coinPrice?.diff,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                receiveEnabled = state != null,
                syncingProgress = getSyncingProgress(state, wallet.coinType),
                syncingTextValue = getSyncingText(state, expanded),
                syncedUntilTextValue = getSyncedUntilText(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                badge = wallet.badge,
                swapVisible = item.wallet.coinType.swappable,
                swapEnabled = state is AdapterState.Synced,
                mainNet = item.mainNet,
                errorMessage = (state as? AdapterState.NotSynced)?.error?.message
        )
    }

    fun headerViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency, balanceHidden: Boolean): BalanceHeaderViewItem = when {
        balanceHidden -> BalanceHeaderViewItem("*****", true)
        else -> {
            val total = items.mapNotNull { item ->
                item.coinPrice?.let { item.balanceData.total.multiply(it.value) }
            }.fold(BigDecimal.ZERO, BigDecimal::add)

            val balanceText = App.numberFormatter.formatFiat(total, currency.symbol, 2, 2)

            val upToDate = !items.any {
                it.state !is AdapterState.Synced || (it.coinPrice != null && it.coinPrice.expired)
            }

            BalanceHeaderViewItem(balanceText, upToDate)
        }
    }

}
