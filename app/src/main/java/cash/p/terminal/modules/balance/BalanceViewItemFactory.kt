package cash.p.terminal.modules.balance

import androidx.compose.runtime.Immutable
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.diffPercentage
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.modules.balance.BalanceModule.warningText
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui.compose.components.diffSign
import cash.p.terminal.ui.compose.components.diffText
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceViewHelper
import cash.p.terminal.wallet.balance.BalanceViewHelper.coinValue
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.math.RoundingMode

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
    val warning: WarningText?,
    val diff: BigDecimal? = null,
    val fullDiff: String = "",
    val displayDiffOptionType: DisplayDiffOptionType = DisplayDiffOptionType.BOTH,
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
    val fullDiff: String,
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
    val displayDiffOptionType: DisplayDiffOptionType,
    val stackingUnpaid: DeemedValue<String>?
)

enum class SyncingProgressType {
    Spinner, ProgressWithRing
}

data class SyncingProgress(val type: SyncingProgressType?, val progress: Int?)

class BalanceViewItemFactory {

    private fun getSyncingProgress(
        state: AdapterState?,
        blockchainType: BlockchainType
    ): SyncingProgress {
        return when (state) {
            is AdapterState.Connecting -> SyncingProgress(SyncingProgressType.Spinner, 10)
            is AdapterState.Syncing -> {
                if (state.substatus != null) {
                    SyncingProgress(SyncingProgressType.Spinner, 10)
                } else {
                    val progress = state.progress
                    val progressValue = progress ?: getDefaultSyncingProgress(blockchainType)
                    if (progress != null && progress > 0 && blockchainType.isSyncWithProgress()) {
                        SyncingProgress(SyncingProgressType.ProgressWithRing, progressValue)
                    } else {
                        SyncingProgress(SyncingProgressType.Spinner, progressValue)
                    }
                }
            }
            is AdapterState.SearchingTxs -> SyncingProgress(SyncingProgressType.Spinner, 10)
            else -> SyncingProgress(null, null)
        }
    }

    private fun BlockchainType.isSyncWithProgress() = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dogecoin,
        BlockchainType.Dash,
        BlockchainType.PirateCash,
        BlockchainType.Cosanta,
        BlockchainType.Zcash,
        BlockchainType.Monero,
        BlockchainType.BinanceSmartChain -> true
        else -> false
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
        return when (state) {
            is AdapterState.Connecting -> Translator.getString(R.string.balance_connecting)
            is AdapterState.Syncing -> getSyncingProgressText(state)
            is AdapterState.SearchingTxs -> {
                if (state.count == 0) {
                    Translator.getString(R.string.balance_connecting_to_api)
                } else {
                    Translator.getString(R.string.Balance_SearchingTransactions)
                }
            }
            else -> null
        }
    }

    private fun getSyncingProgressText(state: AdapterState.Syncing): String {
        val sub = state.substatus
        if (sub is AdapterState.Substatus.WaitingForPeers) {
            return Translator.getString(R.string.balance_waiting_for_peers, sub.connected, sub.required)
        }

        val blocksRemained = state.blocksRemained
        val progress = state.progress
        return when {
            blocksRemained != null && blocksRemained > 0 -> {
                Translator.getString(
                    R.string.balance_syncing_blocks_remaining,
                    formatBlocksRemaining(blocksRemained)
                )
            }
            blocksRemained == null && progress != null && progress >= 100 -> {
                Translator.getString(R.string.balance_processing)
            }
            progress != null && progress > 0 -> {
                Translator.getString(
                    R.string.Balance_Syncing_WithProgress,
                    progress.toString()
                )
            }
            else -> Translator.getString(R.string.Balance_Syncing)
        }
    }

    private fun formatBlocksRemaining(blocks: Long): String {
        val (value, suffix) = when {
            blocks >= 1_000_000 -> (blocks / 1_000_000.0) to Translator.getString(R.string.CoinPage_MarketCap_Million)
            blocks >= 1_000 -> (blocks / 1_000.0) to Translator.getString(R.string.CoinPage_MarketCap_Thousand)
            else -> return blocks.toString()
        }
        val formattedValue = App.numberFormatter.format(value, 0, 1)
        return Translator.getString(R.string.LargeNumberFormat, formattedValue, suffix)
    }

    private fun getSyncedUntilText(state: AdapterState?): String? {
        if (state == null) {
            return null
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.substatus != null) {
                    null
                } else {
                    state.lastBlockDate?.let { date ->
                        Translator.getString(
                            R.string.Balance_SyncedUntil, DateHelper.formatDate(
                                date, "MMM d, yyyy"
                            )
                        )
                    }
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
        isSwappable: Boolean,
        displayDiffOptionType: DisplayDiffOptionType = DisplayDiffOptionType.BOTH,
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

            lockedCoinValue(
                state,
                item.balanceData.minimumBalance,
                hideBalance,
                wallet.decimal,
                wallet.token
            )?.let {
                var info =
                    TranslatableString.ResString(R.string.Info_Reserved_Description).toString()
                info += "\n\n"
                info += TranslatableString.ResString(R.string.Info_Reserved_CurrentlyLocked)
                    .toString()

                info += "\n1 XLM - " + TranslatableString.ResString(R.string.Info_Reserved_WalletAction)
                    .toString()
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
            swapEnabled = state !is AdapterState.NotSynced,
            errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
            isWatchAccount = watchAccount,
            warning = item.warning?.warningText,
            isSendDisabled = sendDisabled,
            isShowShieldFunds = isShowShieldFunds,
            diff = item.coinPrice?.diffPercentage,
            fullDiff = getFullDiff(item, displayDiffOptionType, currency),
            displayDiffOptionType = displayDiffOptionType,
        )
    }

    fun viewItem2(
        item: BalanceItem,
        currency: Currency,
        roundingAmount: Boolean,
        hideBalance: Boolean,
        watchAccount: Boolean,
        isSwipeToDeleteEnabled: Boolean,
        balanceViewType: BalanceViewType,
        networkAvailable: Boolean,
        showStackingUnpaid: Boolean,
        displayDiffOptionType: DisplayDiffOptionType
    ): BalanceViewItem2 {
        val wallet = item.wallet
        val state = item.state
        val latestRate = item.coinPrice

        val balanceTotalVisibility = !hideBalance

        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = item.balanceData.total,
            visible = balanceTotalVisibility,
            fullFormat = !roundingAmount,
            coinDecimals = wallet.decimal,
            dimmed = state !is AdapterState.Synced,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        val stackingUnpaid =
            if (showStackingUnpaid && item.balanceData.stackingUnpaid != BigDecimal.ZERO) {
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
            diff = item.coinPrice?.diffPercentage,
            fullDiff = getFullDiff(item, displayDiffOptionType, currency),
            sendEnabled = item.sendAllowed,
            syncingProgress = getSyncingProgress(state, wallet.token.blockchainType),
            syncingTextValue = getSyncingText(state),
            syncedUntilTextValue = getSyncedUntilText(state),
            failedIconVisible = state is AdapterState.NotSynced,
            badge = wallet.badge,
            swapEnabled = state !is AdapterState.NotSynced,
            errorMessage = errorMessage,
            isWatchAccount = watchAccount,
            isSwipeToDeleteEnabled = isSwipeToDeleteEnabled,
            stackingUnpaid = stackingUnpaid,
            displayDiffOptionType = displayDiffOptionType
        )
    }

    private fun getFullDiff(
        item: BalanceItem,
        displayDiffOptionType: DisplayDiffOptionType,
        currency: Currency,
    ): String {
        val latestRate = item.coinPrice
        val diffPercentage = item.coinPrice?.diffPercentage
        val currentPrice = latestRate?.value ?: BigDecimal.ZERO

        val sign = diffPercentage?.diffSign() ?: ""

        val diffPercentageText = if (displayDiffOptionType.hasPercentChange) {
            diffText(diffPercentage)
        } else {
            ""
        }

        val diffCurrencyText = if (displayDiffOptionType.hasPriceChange && diffPercentage != null) {

            val percentDecimal = diffPercentage.divide(BigDecimal(100))
            val previousPrice = tryOrNull {
                currentPrice.divide(
                    BigDecimal.ONE + percentDecimal,
                    10,
                    RoundingMode.HALF_UP
                )
            } ?: return "-100%"
            val priceChange = currentPrice - previousPrice

            val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
            val formattedPriceChange =
                numberFormatter.formatFiatFull(priceChange.abs(), currency.symbol)
            val signedFormattedPrice = "$sign$formattedPriceChange"

            if (displayDiffOptionType == DisplayDiffOptionType.BOTH) {
                " ($signedFormattedPrice)"
            } else {
                signedFormattedPrice
            }
        } else {
            ""
        }

        return "$diffPercentageText$diffCurrencyText"
    }
}
