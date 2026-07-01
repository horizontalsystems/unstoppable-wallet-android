package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ExternalContractCallTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val incomingEvents: List<TransferEvent>,
    val outgoingEvents: List<TransferEvent>,
    isSpam: Boolean,
    protected: Boolean
) : EvmTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    protected = protected,
    foreignTransaction = true,
    spam = isSpam
) {

    override val mainValue: TransactionValue?
        get() {
            val (incomingValues, outgoingValues) = combined(incomingEvents, outgoingEvents)

            return when {
                (incomingValues.isEmpty() && outgoingValues.size == 1) -> outgoingValues.first()
                (incomingValues.size == 1 && outgoingValues.isEmpty()) -> incomingValues.first()
                else -> null
            }
        }
}
