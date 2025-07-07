package cash.p.terminal.modules.balance

import androidx.compose.runtime.Immutable
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.diff
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.modules.balance.BalanceModule.warningText
import cash.p.terminal.modules.balance.cex.BalanceCexViewItem
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceViewHelper
import cash.p.terminal.wallet.balance.BalanceViewHelper.coinValue
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.models.CoinPrice
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
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
    val isSendDisabled: Boolean,
    val isShowShieldFunds: Boolean,
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
    val isWatchAccount: Boolean,
    val isSwipeToDeleteEnabled: Boolean,
    val stackingUnpaid: DeemedValue<String>?
)

data class SyncingProgress(val progress: Int?, val dimmed: Boolean = false)

class BalanceViewItemFactory {

    private fun getSyncingProgress(
        state: AdapterState?,
        blockchainType: BlockchainType
    ): SyncingProgress {
        return when (state) {
            is AdapterState.Syncing -> SyncingProgress(
                state.progress ?: getDefaultSyncingProgress(
                    blockchainType
                ), false
            )

            is AdapterState.SearchingTxs -> SyncingProgress(10, true)
            else -> SyncingProgress(null, false)
        }
    }

    private fun getDefaultSyncingProgress(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dogecoin,
        BlockchainType.Dash,
        BlockchainType.PirateCash,
        BlockchainType.Cosanta,
        BlockchainType.Monero,
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
                    Translator.getString(
                        R.string.Balance_Syncing_WithProgress,
                        state.progress.toString()
                    )
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
                    Translator.getString(
                        R.string.Balance_SyncedUntil, DateHelper.formatDate(
                            state.lastBlockDate!!, "MMM d, yyyy"
                        )
                    )
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
        item: BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        balanceViewType: BalanceViewType,
        isSwappable: Boolean
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
                state = state,
                balance = item.balanceData.timeLocked,
                hideBalance = hideBalance,
                coinDecimals = wallet.decimal,
                token = wallet.token
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
                state = state,
                balance = item.balanceData.pending,
                hideBalance = hideBalance,
                coinDecimals = wallet.decimal,
                token = wallet.token
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
        }

        val sendDisabled =
            (item.wallet.token.type as? TokenType.AddressSpecTyped)?.type == TokenType.AddressSpecType.Transparent
        val isShowShieldFunds =
            (item.wallet.token.type as? TokenType.AddressSpecTyped)?.type == TokenType.AddressSpecType.Transparent &&
                    item.balanceData.total > ZcashAdapter.MINERS_FEE

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
            swapVisible = isSwappable,
            swapEnabled = state is AdapterState.Synced,
            errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
            isWatchAccount = watchAccount,
            warning = item.warning?.warningText,
            isSendDisabled = sendDisabled,
            isShowShieldFunds = isShowShieldFunds
        )
    }

    fun viewItem2(
        item: BalanceItem,
        currency: Currency,
        hideBalance: Boolean,
        watchAccount: Boolean,
        isSwipeToDeleteEnabled: Boolean,
        balanceViewType: BalanceViewType,
        networkAvailable: Boolean,
        showStackingUnpaid: Boolean
    ): BalanceViewItem2 {
        val wallet = item.wallet
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

        val stackingUnpaid = if (showStackingUnpaid && item.balanceData.stackingUnpaid != BigDecimal.ZERO) {
            coinValue(
                balance = item.balanceData.stackingUnpaid,
                visible = balanceTotalVisibility,
                fullFormat = false,
                coinDecimals = wallet.decimal,
                dimmed = state !is AdapterState.Synced
            ).run {
                copy(
                    value = this.value + " " + wallet.token.coin.code.uppercase()
                )
            }
        } else {
            null
        }

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
            isWatchAccount = watchAccount,
            isSwipeToDeleteEnabled = isSwipeToDeleteEnabled,
            stackingUnpaid = stackingUnpaid
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
            coin = cexAsset.coin,
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
