package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.swappable
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.cex.BalanceCexViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Immutable
data class BalanceViewItem(
    val wallet: Wallet,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val secondaryValue: DeemedValue<String>,
    val coinValueLocked: DeemedValue<String?>,
    val fiatValueLocked: DeemedValue<String>,
    val sendEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: String?,
    val syncedUntilTextValue: String?,
    val failedIconVisible: Boolean,
    val coinIconVisible: Boolean,
    val badge: String?,
    val swapVisible: Boolean,
    val swapEnabled: Boolean = false,
    val errorMessage: String?,
    val isWatchAccount: Boolean
)

@Immutable
data class BalanceViewItem2(
    val wallet: Wallet,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val sendEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val failedIconVisible: Boolean,
    val badge: String?,
    val swapEnabled: Boolean = false,
    val errorMessage: String?,
    val isWatchAccount: Boolean
)

data class DeemedValue<T>(val value: T, val dimmed: Boolean = false, val visible: Boolean = true)
data class SyncingProgress(val progress: Int?, val dimmed: Boolean = false)

class BalanceViewItemFactory {

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

    private fun getSyncingText(state: AdapterState?): String? {
        if (state == null) {
            return null
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
                when (state.zcashState) {
                    is ZcashAdapter.ZcashState.DownloadingBlocks -> Translator.getString(R.string.Balance_DownloadingBlocks)
                    is ZcashAdapter.ZcashState.ScanningBlocks -> Translator.getString(R.string.Balance_ScanningBlocks)
                }
            }
            else -> null
        }

        return text
    }

    private fun getSyncedUntilText(state: AdapterState?): String? {
        if (state == null) {
            return null
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

        return text
    }

    private fun lockedCoinValue(
        state: AdapterState?,
        balance: BigDecimal,
        hideBalance: Boolean,
        coinDecimals: Int,
        token: Token
    ): DeemedValue<String?> {
        if (balance <= BigDecimal.ZERO) {
            return DeemedValue(null, false, false)
        }

        val visible = !hideBalance
        val deemed = state !is AdapterState.Synced

        val value = App.numberFormatter.formatCoinFull(balance, token.coin.code, coinDecimals)

        return DeemedValue(value, deemed, visible)
    }

    fun viewItem(
        item: BalanceModule.BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType
    ): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.coinPrice

        val balanceTotalVisibility = !hideBalance
        val fiatLockedVisibility = !hideBalance && item.balanceData.locked > BigDecimal.ZERO

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            visible = balanceTotalVisibility,
            fullFormat = true,
            coinDecimals = wallet.decimal,
            dimmed = state !is AdapterState.Synced,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        return BalanceViewItem(
            wallet = item.wallet,
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
            fiatValueLocked = BalanceViewHelper.currencyValue(
                item.balanceData.locked,
                latestRate,
                fiatLockedVisibility,
                true,
                currency,
                state !is AdapterState.Synced
            ),
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            sendEnabled = state is AdapterState.Synced,
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = state is AdapterState.NotSynced,
            coinIconVisible = state !is AdapterState.NotSynced,
            badge = wallet.badge,
            swapVisible = wallet.token.swappable,
            swapEnabled = state is AdapterState.Synced,
            errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
            isWatchAccount = watchAccount
        )
    }

    fun viewItem2(
        item: BalanceModule.BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType,
        networkAvailable: Boolean
    ): BalanceViewItem2 {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.coinPrice

        val balanceTotalVisibility = !hideBalance

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            visible = balanceTotalVisibility,
            fullFormat = false,
            coinDecimals = wallet.decimal,
            dimmed = state !is AdapterState.Synced,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        val errorMessage = if (networkAvailable) {
            (state as? AdapterState.NotSynced)?.error?.message
        } else {
            Translator.getString(R.string.Hud_Text_NoInternet)
        }

        return BalanceViewItem2(
            wallet = item.wallet,
            coinCode = coin.code,
            coinTitle = coin.name,
            coinIconUrl = coin.imageUrl,
            coinIconPlaceholder = wallet.token.iconPlaceholder,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            diff = item.coinPrice?.diff,
            sendEnabled = state is AdapterState.Synced,
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            failedIconVisible = state is AdapterState.NotSynced,
            badge = wallet.badge,
            swapEnabled = state is AdapterState.Synced,
            errorMessage = errorMessage,
            isWatchAccount = watchAccount
        )
    }

    fun cexViewItem(
        cexAsset: CexAsset,
        currency: Currency,
        latestRate: CoinPrice?,
        hideBalance: Boolean,
        balanceViewType: BalanceViewType,
        fullFormat: Boolean,
        adapterState: AdapterState
    ): BalanceCexViewItem {
        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = cexAsset.freeBalance + cexAsset.lockedBalance,
            visible = !hideBalance,
            fullFormat = fullFormat,
            coinDecimals = cexAsset.decimals,
            dimmed = false,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )
        val fiatLockedVisibility = !hideBalance && cexAsset.lockedBalance > BigDecimal.ZERO
        val errorMessage = (adapterState as? AdapterState.NotSynced)?.error?.message

        return BalanceCexViewItem(
            coinIconUrl = cexAsset.coin?.imageUrl,
            coinIconPlaceholder = R.drawable.coin_placeholder,
            coinCode = cexAsset.id,
            badge = null,
            primaryValue = primaryValue,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            diff = latestRate?.diff,
            secondaryValue = secondaryValue,
            coinValueLocked = lockedCoinValue(
                balance = cexAsset.lockedBalance,
                hideBalance = hideBalance,
                coinDecimals = cexAsset.decimals,
                coinCode = cexAsset.id
            ),
            fiatValueLocked = BalanceViewHelper.currencyValue(
                balance = cexAsset.lockedBalance,
                coinPrice = latestRate,
                visible = fiatLockedVisibility,
                fullFormat = fullFormat,
                currency = currency,
                dimmed = false
            ),
            coinUid = cexAsset.coin?.uid,
            assetId = cexAsset.id,
            cexAsset = cexAsset,
            coinPrice = latestRate,
            depositEnabled = cexAsset.depositEnabled,
            withdrawEnabled = cexAsset.withdrawEnabled,
            syncingProgress = when (adapterState) {
                is AdapterState.Syncing -> SyncingProgress(50)
                else -> SyncingProgress(null)
            },
            failedIconVisible = adapterState is AdapterState.NotSynced,
            errorMessage = errorMessage,
            adapterState = adapterState
        )
    }

    private fun lockedCoinValue(
        balance: BigDecimal,
        hideBalance: Boolean,
        coinDecimals: Int,
        coinCode: String
    ): DeemedValue<String?> {
        if (balance <= BigDecimal.ZERO) {
            return DeemedValue(null, false, false)
        }
        val visible = !hideBalance
        val value = App.numberFormatter.formatCoinFull(balance, coinCode, coinDecimals)

        return DeemedValue(value, false, visible)
    }

}
