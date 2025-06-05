package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.diff
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.swappable
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.warningText
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Immutable
data class BalanceViewItem(
    val wallet: Wallet,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val secondaryValue: DeemedValue<String>,
    val lockedValues: List<LockedValue>,
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
    val isWatchAccount: Boolean,
    val warning: WarningText?
)

data class WarningText(
    val title: TranslatableString,
    val text: TranslatableString
)

data class LockedValue(
    val title: TranslatableString,
    val infoTitle: TranslatableString,
    val info: TranslatableString,
    val coinValue: DeemedValue<String>
)

@Immutable
data class BalanceViewItem2(
    val wallet: Wallet,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val sendEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: String?,
    val syncedUntilTextValue: String?,
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
            is AdapterState.SearchingTxs -> SyncingProgress(10, true)
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
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Solana,
        BlockchainType.Tron,
        BlockchainType.Stellar,
        BlockchainType.Ton -> 50
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
    ): DeemedValue<String>? {
        if (balance <= BigDecimal.ZERO) {
            return null
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
        val state = item.state
        val latestRate = item.coinPrice

        val balanceTotalVisibility = !hideBalance

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

        val lockedValues = buildList {
            lockedCoinValue(
                state,
                item.balanceData.timeLocked,
                hideBalance,
                wallet.decimal,
                wallet.token
            )?.let {
                add(
                    LockedValue(
                        title = TranslatableString.ResString(R.string.Balance_LockedAmount_Title),
                        infoTitle = TranslatableString.ResString(R.string.Info_LockTime_Title),
                        info = TranslatableString.ResString(R.string.Info_ProcessingBalance_Description),
                        coinValue = it
                    )
                )
            }

            lockedCoinValue(
                state,
                item.balanceData.pending,
                hideBalance,
                wallet.decimal,
                wallet.token
            )?.let {
                add(
                    LockedValue(
                        title = TranslatableString.ResString(R.string.Balance_ProcessingBalance_Title),
                        infoTitle = TranslatableString.ResString(R.string.Info_ProcessingBalance_Title),
                        info = TranslatableString.ResString(R.string.Info_ProcessingBalance_Description),
                        coinValue = it
                    )
                )
            }

            lockedCoinValue(
                state,
                item.balanceData.notRelayed,
                hideBalance,
                wallet.decimal,
                wallet.token
            )?.let {
                add(
                    LockedValue(
                        title = TranslatableString.ResString(R.string.Balance_NotRelayedAmount_Title),
                        infoTitle = TranslatableString.ResString(R.string.Info_NotRelayed_Title),
                        info = TranslatableString.ResString(R.string.Info_NotRelayed_Description),
                        coinValue = it
                    )
                )
            }

            lockedCoinValue(
                state,
                item.balanceData.minimumBalance,
                hideBalance,
                wallet.decimal,
                wallet.token
            )?.let {
                var info = TranslatableString.ResString(R.string.Info_Reserved_Description).toString()
                info += "\n\n"
                info += TranslatableString.ResString(R.string.Info_Reserved_CurrentlyLocked).toString()

                info += "\n1 XLM - " + TranslatableString.ResString(R.string.Info_Reserved_WalletAction).toString()
                item.balanceData.stellarAssets.forEach {
                    info += "\n0.5 XLM - ${it.code}"
                }

                add(
                    LockedValue(
                        title = TranslatableString.ResString(R.string.Balance_Reserved_Title),
                        infoTitle = TranslatableString.ResString(R.string.Info_Reserved_Title),
                        info = TranslatableString.PlainString(info),
                        coinValue = it
                    )
                )
            }
        }

        return BalanceViewItem(
            wallet = item.wallet,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            lockedValues = lockedValues,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            sendEnabled = item.sendAllowed,
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = state is AdapterState.NotSynced,
            coinIconVisible = state !is AdapterState.NotSynced,
            badge = wallet.badge,
            swapVisible = App.instance.isSwapEnabled && wallet.token.swappable,
            swapEnabled = state is AdapterState.Synced,
            errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
            isWatchAccount = watchAccount,
            warning = item.warning?.warningText
        )
    }

    fun viewItem2(
        item: BalanceModule.BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType,
        networkAvailable: Boolean,
        amountRoundingEnabled: Boolean
    ): BalanceViewItem2 {
        val wallet = item.wallet
        val state = item.state
        val latestRate = item.coinPrice

        val balanceTotalVisibility = !hideBalance

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            visible = balanceTotalVisibility,
            fullFormat = !amountRoundingEnabled,
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
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            diff = item.coinPrice?.diff,
            sendEnabled = item.sendAllowed,
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = state is AdapterState.NotSynced,
            badge = wallet.badge,
            swapEnabled = state is AdapterState.Synced,
            errorMessage = errorMessage,
            isWatchAccount = watchAccount
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
