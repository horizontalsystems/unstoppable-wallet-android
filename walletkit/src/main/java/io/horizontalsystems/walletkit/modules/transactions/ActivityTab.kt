package io.horizontalsystems.walletkit.modules.transactions

import androidx.annotation.StringRes
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import kotlinx.serialization.Serializable

@Serializable
enum class ActivityTab(@StringRes val title: Int, @StringRes val emptyText: Int) {
    Transfers(R.string.Activity_Tab_Transfers, R.string.Activity_Empty_Transfers),
    Swaps(R.string.Transactions_Swaps, R.string.Activity_Empty_Swaps),
    Privates(R.string.Activity_Tab_Privates, R.string.Activity_Empty_PrivateSends),
    CrossPays(R.string.Activity_Tab_CrossPay, R.string.Activity_Empty_CrossPay);

    // Transfers lists on-chain transactions only; the other tabs list swap records only.
    fun shows(kind: SwapRecordKind): Boolean = when (this) {
        Transfers -> false
        Swaps -> kind == SwapRecordKind.Swap || kind == SwapRecordKind.PrivateSwap
        Privates -> kind == SwapRecordKind.PrivateSend
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
 * The filter's coin, blockchain and contact applied to a swap record: a record matches when
 * either of its sides does. With a blockchain chosen, only the contact's addresses on that
 * blockchain count.
 */
internal fun SwapRecord.matchesActivityFilter(tokenQueryId: String?, blockchainUid: String?, contact: Contact?): Boolean {
    if (tokenQueryId != null) {
        if (tokenInUid != tokenQueryId && tokenOutUid != tokenQueryId) return false
    } else if (blockchainUid != null) {
        if (tokenInBlockchainTypeUid != blockchainUid && tokenOutBlockchainTypeUid != blockchainUid) return false
    }

    if (contact != null) {
        val addresses = contact.addresses
            .filter { blockchainUid == null || it.blockchain.uid == blockchainUid }
            .map { it.address.lowercase() }
        val recordAddresses = listOfNotNull(recipientAddress, sourceAddress).map { it.lowercase() }
        if (recordAddresses.none { it in addresses }) return false
    }

    return true
}
