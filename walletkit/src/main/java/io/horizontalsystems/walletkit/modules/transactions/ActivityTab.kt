package io.horizontalsystems.walletkit.modules.transactions

import androidx.annotation.StringRes
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
enum class ActivityTab(@StringRes val title: Int) {
    Transfers(R.string.Activity_Tab_Transfers),
    Swaps(R.string.Transactions_Swaps),
    Privates(R.string.Activity_Tab_Privates),
    CrossPays(R.string.Activity_Tab_CrossPays);

    // Transfers lists every on-chain transaction as well as every swap record.
    fun shows(kind: SwapRecordKind): Boolean = when (this) {
        Transfers -> true
        Swaps -> kind == SwapRecordKind.Swap || kind == SwapRecordKind.PrivateSwap
        Privates -> kind == SwapRecordKind.PrivateSwap || kind == SwapRecordKind.PrivateSend
        CrossPays -> kind == SwapRecordKind.CrossPay
    }
}

/** What a swap record is to the user, which decides its tab and its row. */
enum class SwapRecordKind {
    Swap, PrivateSwap, PrivateSend, CrossPay;

    companion object {
        // A swap is private when it ran on the swap screen's Private tab (a split swap) or
        // through a confidential provider quoted beside the public ones on the Standard tab.
        fun of(operation: SwapOperation, providerId: String, confidentialProviderIds: Set<String>) = when (operation) {
            SwapOperation.SplitSwap -> PrivateSwap
            SwapOperation.Swap -> if (providerId in confidentialProviderIds) PrivateSwap else Swap
            SwapOperation.PrivateSend -> PrivateSend
            SwapOperation.CrossPay -> CrossPay
        }
    }
}

internal fun swapRecordIconState(status: SwapStatus) = when (status) {
    SwapStatus.Depositing,
    SwapStatus.Swapping,
    SwapStatus.Sending -> TransactionIconState.Pending

    SwapStatus.Completed -> TransactionIconState.Completed

    // Every way a record ends without the swap done is shown alike, though a refund
    // returned the funds and an expired order was never funded.
    SwapStatus.Failed,
    SwapStatus.Refunded,
    SwapStatus.Expired,
    SwapStatus.ActionRequired -> TransactionIconState.Failed
}

/**
 * The Transfers list: on-chain transactions and swap records by date, newest first. A swap
 * record's deposit is also an on-chain transaction; the record stands for both, so the
 * transaction is dropped.
 */
internal fun <T> mergeActivity(
    transactions: List<Pair<T, String>>,
    swapRecords: List<T>,
    depositHashes: Set<String>,
    date: (T) -> Date,
): List<T> {
    val normalizedHashes = depositHashes.mapTo(HashSet()) { it.lowercase() }
    val shownTransactions = transactions
        .filter { (_, hash) -> hash.lowercase() !in normalizedHashes }
        .map { it.first }

    return (shownTransactions + swapRecords).sortedByDescending(date)
}
