package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState

sealed class TransactionType {
    class Incoming(
        val from: String?,
        val amount: String,
        val lockState: TransactionLockState?,
        val conflictingTxHash: String?
    ) : TransactionType()

    class Outgoing(
        val to: String?,
        val amount: String,
        val lockState: TransactionLockState?,
        val conflictingTxHash: String?,
        val sentToSelf: Boolean
    ) : TransactionType()

    class Approve(
        val spender: String,
        val amount: String,
        val coinCode: String,
        val isMaxAmount: Boolean
    ) :
        TransactionType()

    class Swap(
        val exchangeAddress: String,
        val amountIn: String,
        val amountOut: String?,
        val foreignRecipient: Boolean
    ) : TransactionType()

    class ContractCall(val contractAddress: String, val method: String?) : TransactionType()
    object ContractCreation : TransactionType()

    override fun equals(other: Any?): Boolean {
        return when {
            other is Incoming && this is Incoming -> other.lockState == this.lockState && other.conflictingTxHash == this.conflictingTxHash
            other is Outgoing && this is Outgoing -> other.lockState == this.lockState && other.conflictingTxHash == this.conflictingTxHash
            other is Approve && this is Approve -> true
            other is Swap && this is Swap -> true
            other is ContractCall && this is ContractCall -> true
            other is ContractCreation && this is ContractCreation -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

