package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction

class ApproveTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val spender: String,
    val value: TransactionValue
) : EvmTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
