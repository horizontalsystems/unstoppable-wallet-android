package cash.p.terminal.entities.transactionrecords.tron

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.tronkit.models.Transaction

class TronApproveTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val spender: String,
    val value: TransactionValue
) : TronTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
