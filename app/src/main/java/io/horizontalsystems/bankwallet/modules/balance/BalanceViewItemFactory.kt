package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ZcashBalanceData
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
    val primaryValue: DeemedValue<String>?,
    val exchangeValue: DeemedValue<String>?,
    val secondaryValue: DeemedValue<String>?,
    val lockedValues: List<LockedValue>,
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
    val warning: WarningText?,
    val balanceHidden: Boolean
) {
    val syncingLineText = syncingTextValue?.let {
        buildString {
            append(syncingTextValue)

            syncedUntilTextValue?.let {
                append(" - $syncedUntilTextValue")
            }
        }
    }
}

data class WarningText(
    val title: TranslatableString,
    val text: TranslatableString
)

open class LockedValue(
    val title: TranslatableString,
    val infoTitle: TranslatableString,
    val info: TranslatableString,
    val coinValue: DeemedValue<String>
)

class ZcashLockedValue(
    title: TranslatableString,
    infoTitle: TranslatableString,
    info: TranslatableString,
    coinValue: DeemedValue<String>
) : LockedValue(title, infoTitle, info, coinValue)

@Immutable
data class BalanceViewItem2(
    val wallet: Wallet,
    val primaryValue: DeemedValue<String>?,
    val exchangeValue: DeemedValue<String>?,
    val diff: DeemedValue<BigDecimal>?,
    val secondaryValue: DeemedValue<String>?,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: String?,
    val syncedUntilTextValue: String?,
    val failedIconVisible: Boolean,
    val badge: String?,
    val errorMessage: String?,
    val isWatchAccount: Boolean,
    val balanceHidden: Boolean
)

data class DeemedValue<T>(val value: T, val dimmed: Boolean = false)
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
        BlockchainType.Zcash,
        BlockchainType.Monero -> 10

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
        coinDecimals: Int,
        token: Token
    ): DeemedValue<String>? {
        if (balance <= BigDecimal.ZERO) {
            return null
        }

        val deemed = state !is AdapterState.Synced

        val value = App.numberFormatter.formatCoinFull(balance, token.coin.code, coinDecimals)

        return DeemedValue(value, deemed)
    }

    fun viewItem(
        item: BalanceModule.BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType,
        networkAvailable: Boolean
    ): BalanceViewItem {
        val wallet = item.wallet
        val state = item.state
        val latestRate = item.coinPrice

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            fullFormat = true,
            coinDecimals = wallet.decimal,
            dimmed = networkAvailable && state !is AdapterState.Synced,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        val lockedValues = buildList {
            lockedCoinValue(
                state,
                item.balanceData.timeLocked,
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

            if (item.balanceData is ZcashBalanceData) {
                lockedCoinValue(
                    state,
                    item.balanceData.unshielded,
                    wallet.decimal,
                    wallet.token
                )?.let {
                    add(
                        ZcashLockedValue(
                            title = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalance_Title),
                            infoTitle = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalance_Info_Title),
                            info = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalance_Info_Description),
                            coinValue = it
                        )
                    )
                }
            }
        }

        val errorMessage = if (networkAvailable) {
            (state as? AdapterState.NotSynced)?.error?.message
        } else {
            Translator.getString(R.string.Hud_Text_NoInternet)
        }

        return BalanceViewItem(
            wallet = item.wallet,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            lockedValues = lockedValues,
            exchangeValue = latestRate?.let { BalanceViewHelper.rateValue(it, currency) },
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = networkAvailable && state is AdapterState.NotSynced,
            coinIconVisible = state !is AdapterState.NotSynced,
            badge = wallet.badge,
            swapVisible = App.instance.isSwapEnabled && wallet.token.swappable,
            errorMessage = errorMessage,
            isWatchAccount = watchAccount,
            warning = item.warning?.warningText,
            balanceHidden = hideBalance
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

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            fullFormat = !amountRoundingEnabled,
            coinDecimals = wallet.decimal,
            dimmed = networkAvailable && state !is AdapterState.Synced,
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
            exchangeValue = latestRate?.let { BalanceViewHelper.rateValue(it, currency) },
            diff = latestRate?.diff?.let { DeemedValue(it, latestRate.expired) },
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = networkAvailable && state is AdapterState.NotSynced,
            badge = wallet.badge,
            errorMessage = errorMessage,
            isWatchAccount = watchAccount,
            balanceHidden = hideBalance
        )
    }
}
