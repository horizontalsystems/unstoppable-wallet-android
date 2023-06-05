package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.swappable
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Immutable
data class BalanceViewItem(
    val wallet: Wallet,
    val currencySymbol: String,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val coinValueLocked: DeemedValue<String>,
    val fiatValueLocked: DeemedValue<String>,
    val expanded: Boolean,
    val sendEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: DeemedValue<String?>,
    val syncedUntilTextValue: DeemedValue<String?>,
    val failedIconVisible: Boolean,
    val coinIconVisible: Boolean,
    val badge: String?,
    val swapVisible: Boolean,
    val swapEnabled: Boolean = false,
    val errorMessage: String?,
    val isWatchAccount: Boolean
)

data class DeemedValue<T>(val value: T, val dimmed: Boolean = false, val visible: Boolean = true)
data class SyncingProgress(val progress: Int?, val dimmed: Boolean = false)

class BalanceViewItemFactory {

    private fun coinValue(
        state: AdapterState?,
        balance: BigDecimal,
        visible: Boolean,
        full: Boolean,
        coinDecimals: Int
    ): DeemedValue<String> {
        val dimmed = state !is AdapterState.Synced
        val formatted = if (full) {
            App.numberFormatter.formatCoinFull(balance, null, coinDecimals)
        } else {
            App.numberFormatter.formatCoinShort(balance, null, coinDecimals)
        }

        return DeemedValue(formatted, dimmed, visible)
    }

    private fun currencyValue(
        state: AdapterState?,
        balance: BigDecimal,
        coinPrice: CoinPrice?,
        visible: Boolean,
        fullFormat: Boolean,
        currency: Currency
    ): DeemedValue<String> {
        val dimmed = state !is AdapterState.Synced || coinPrice?.expired ?: false
        val formatted = coinPrice?.value?.let { rate ->
            val balanceFiat = balance.multiply(rate)

            if (fullFormat) {
                App.numberFormatter.formatFiatFull(balanceFiat, currency.symbol)
            } else {
                App.numberFormatter.formatFiatShort(balanceFiat, currency.symbol, 8)
            }
        } ?: ""

        return DeemedValue(formatted, dimmed, visible)
    }

    private fun rateValue(coinPrice: CoinPrice?, showSyncing: Boolean, currency: Currency): DeemedValue<String> {
        val value = coinPrice?.let {
            App.numberFormatter.formatFiatFull(coinPrice.value, currency.symbol)
        } ?: ""

        return DeemedValue(value, dimmed = coinPrice?.expired ?: false, visible = !showSyncing)
    }

    private fun getSyncingProgress(state: AdapterState?, blockchainType: BlockchainType): SyncingProgress {
        return when (state) {
            is AdapterState.Syncing -> SyncingProgress(state.progress ?: getDefaultSyncingProgress(blockchainType), false)
            is AdapterState.SearchingTxs, is AdapterState.Zcash -> SyncingProgress(10, true)
            else -> SyncingProgress(null, false)
        }
    }

    private fun getDefaultSyncingProgress(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.Zcash -> 10
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.BinanceChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Solana,
        BlockchainType.Tron -> 50
        is BlockchainType.Unsupported -> 0
    }

    private fun getSyncingText(state: AdapterState?, expanded: Boolean): DeemedValue<String?> {
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
            is AdapterState.Zcash -> {
                when(state.zcashState){
                    is ZcashAdapter.ZcashState.DownloadingBlocks -> Translator.getString(R.string.Balance_DownloadingBlocks)
                    is ZcashAdapter.ZcashState.ScanningBlocks -> Translator.getString(R.string.Balance_ScanningBlocks)
                }
            }
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun getSyncedUntilText(state: AdapterState?, expanded: Boolean): DeemedValue<String?> {
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
            is AdapterState.Zcash -> {
                when (val zcash = state.zcashState) {
                    is ZcashAdapter.ZcashState.DownloadingBlocks -> {
                        if (zcash.blockProgress.current != null && zcash.blockProgress.total != null) {
                            "${zcash.blockProgress.current}/${zcash.blockProgress.total}"
                        } else {
                            ""
                        }
                    }
                    is ZcashAdapter.ZcashState.ScanningBlocks -> {
                        if (zcash.blockProgress.current != null && zcash.blockProgress.total != null) {
                            "${zcash.blockProgress.current}/${zcash.blockProgress.total}"
                        } else {
                            ""
                        }
                    }
                }
            }
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun lockedCoinValue(
        state: AdapterState?,
        balance: BigDecimal,
        hideBalance: Boolean,
        coinDecimals: Int,
        token: Token
    ): DeemedValue<String> {
        val visible = !hideBalance && balance > BigDecimal.ZERO
        val deemed = state !is AdapterState.Synced

        val value = App.numberFormatter.formatCoinFull(balance, token.coin.code, coinDecimals)

        return DeemedValue(value, deemed, visible)
    }

    fun viewItem(
        item: BalanceModule.BalanceItem,
        currency: Currency,
        expanded: Boolean,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType
    ): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.coinPrice

        val showSyncing = expanded && (state is AdapterState.Syncing || state is AdapterState.SearchingTxs || state is AdapterState.Zcash)
        val balanceTotalVisibility = !hideBalance && !showSyncing
        val fiatLockedVisibility = !hideBalance && item.balanceData.locked > BigDecimal.ZERO

        val coinValueStr = coinValue(
            state,
            item.balanceData.total,
            balanceTotalVisibility,
            expanded,
            wallet.decimal
        )
        val currencyValueStr = currencyValue(
            state,
            item.balanceData.total,
            latestRate,
            balanceTotalVisibility,
            expanded,
            currency
        )

        val primaryValue: DeemedValue<String>
        val secondaryValue: DeemedValue<String>
        when (balanceViewType) {
            BalanceViewType.CoinThenFiat -> {
                primaryValue = coinValueStr
                secondaryValue = currencyValueStr
            }
            BalanceViewType.FiatThenCoin -> {
                primaryValue = currencyValueStr
                secondaryValue = coinValueStr
            }
        }

        return BalanceViewItem(
                wallet = item.wallet,
                currencySymbol = currency.symbol,
                coinCode = coin.code,
                coinTitle = coin.name,
                coinIconUrl = coin.imageUrl,
                coinIconPlaceholder = wallet.token.iconPlaceholder,
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                coinValueLocked = lockedCoinValue(
                    state,
                    item.balanceData.locked,
                    hideBalance,
                    wallet.decimal,
                    wallet.token
                ),
                fiatValueLocked = currencyValue(
                    state,
                    item.balanceData.locked,
                    latestRate,
                    fiatLockedVisibility,
                    true,
                    currency
                ),
                exchangeValue = rateValue(latestRate, showSyncing, currency),
                diff = item.coinPrice?.diff,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
                syncingTextValue = getSyncingText(state, expanded),
                syncedUntilTextValue = getSyncedUntilText(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                badge = wallet.badge,
                swapVisible = wallet.token.swappable,
                swapEnabled = state is AdapterState.Synced,
                errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
                isWatchAccount = watchAccount
        )
    }
}
