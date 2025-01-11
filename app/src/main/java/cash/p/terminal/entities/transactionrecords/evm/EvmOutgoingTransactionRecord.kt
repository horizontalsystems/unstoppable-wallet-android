package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction

class EvmOutgoingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean
) : EvmTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
