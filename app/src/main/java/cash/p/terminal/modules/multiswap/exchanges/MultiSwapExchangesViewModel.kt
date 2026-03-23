package cash.p.terminal.modules.multiswap.exchanges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.modules.multiswap.exchange.LegStatus
import cash.p.terminal.modules.multiswap.exchange.MultiSwapExchangeViewModel.Companion.mapStatus
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.changenow.domain.entity.toStatus
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.coinImageUrl
import io.horizontalsystems.core.IAppNumberFormatter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MultiSwapExchangesViewModel(
    private val pendingMultiSwapStorage: PendingMultiSwapStorage,
    private val marketKit: MarketKitWrapper,
    private val numberFormatter: IAppNumberFormatter,
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage,
) : ViewModel() {

    var uiState by mutableStateOf(MultiSwapExchangesUiState())
        private set

    init {
        observeSwaps()
    }

    private fun observeSwaps() {
        viewModelScope.launch {
            combine(
                pendingMultiSwapStorage.getAll(),
                swapProviderTransactionsStorage.observeAll(),
            ) { swaps, _ -> swaps }
                .collect { swaps ->
                    uiState = MultiSwapExchangesUiState(
                        items = swaps.map { mapToCardItem(it) }
                    )
                }
        }
    }

    private fun mapToCardItem(swap: PendingMultiSwap): ExchangeCardItem {
        val leg1Status = mapStatus(swap.leg1Status)
        val leg2Status = mapStatus(swap.leg2Status)

        val coinCodeIn = coinCode(swap.coinUidIn)
        val coinCodeIntermediate = coinCode(swap.coinUidIntermediate)
        val coinCodeOut = coinCode(swap.coinUidOut)

        val activeLine = when {
            leg1Status != LegStatus.Completed -> 0
            leg2Status != LegStatus.Completed -> 1
            else -> 2
        }

        return ExchangeCardItem(
            id = swap.id,
            coinIconUrlIn = coinImageUrl(swap.coinUidIn),
            coinIconUrlIntermediate = coinImageUrl(swap.coinUidIntermediate),
            coinIconUrlOut = coinImageUrl(swap.coinUidOut),
            amountInFormatted = numberFormatter.formatCoinFull(swap.amountIn, coinCodeIn, 8),
            amountIntermediateFormatted = swap.leg1AmountOut?.let {
                numberFormatter.formatCoinFull(it, coinCodeIntermediate, 8)
            } ?: "--- $coinCodeIntermediate",
            amountOutFormatted = numberFormatter.formatCoinFull(
                swap.leg2AmountOut ?: swap.expectedAmountOut, coinCodeOut, 8
            ),
            activeLine = activeLine,
            statusText = resolveStatusText(swap, leg1Status, leg2Status),
        )
    }

    private fun resolveStatusText(
        swap: PendingMultiSwap,
        leg1Status: LegStatus,
        leg2Status: LegStatus,
    ): String {
        if (leg1Status == LegStatus.Completed && leg2Status == LegStatus.Completed) {
            return Translator.getString(R.string.multi_swap_completed)
        }
        if (leg1Status == LegStatus.Completed && leg2Status == LegStatus.Pending) {
            return Translator.getString(R.string.multi_swap_continue)
        }
        val (txId, isOffChain) = when {
            leg1Status == LegStatus.Executing -> swap.leg1TransactionId to swap.leg1IsOffChain
            leg2Status == LegStatus.Executing -> swap.leg2TransactionId to (swap.leg2IsOffChain ?: false)
            else -> null to false
        }
        if (isOffChain && txId != null) {
            val swapTx = swapProviderTransactionsStorage.getByOutgoingRecordUid(txId)
            if (swapTx != null) {
                return Translator.getString(statusToStringRes(swapTx.status.toStatus()))
            }
        }
        return Translator.getString(R.string.transaction_swap_status_confirming)
    }

    private fun coinCode(uid: String) = marketKit.coin(uid)?.code ?: uid

    fun onDelete(id: String) {
        viewModelScope.launch {
            pendingMultiSwapStorage.delete(id)
        }
    }
}

data class MultiSwapExchangesUiState(
    val items: List<ExchangeCardItem> = emptyList(),
)

data class ExchangeCardItem(
    val id: String,
    val coinIconUrlIn: String?,
    val coinIconUrlIntermediate: String?,
    val coinIconUrlOut: String?,
    val amountInFormatted: String,
    val amountIntermediateFormatted: String,
    val amountOutFormatted: String,
    val activeLine: Int,
    val statusText: String,
)

private fun statusToStringRes(status: TransactionStatusEnum): Int = when (status) {
    TransactionStatusEnum.NEW -> R.string.transaction_swap_status_new
    TransactionStatusEnum.WAITING -> R.string.transaction_swap_status_waiting
    TransactionStatusEnum.CONFIRMING -> R.string.transaction_swap_status_confirming
    TransactionStatusEnum.EXCHANGING -> R.string.transaction_swap_status_exchanging
    TransactionStatusEnum.SENDING -> R.string.transaction_swap_status_sending
    TransactionStatusEnum.FINISHED -> R.string.multi_swap_completed
    TransactionStatusEnum.FAILED -> R.string.Transactions_Failed
    TransactionStatusEnum.REFUNDED -> R.string.transaction_swap_status_refunded
    TransactionStatusEnum.VERIFYING -> R.string.transaction_swap_status_verifying
    TransactionStatusEnum.UNKNOWN -> R.string.transaction_swap_status_unknown
}
