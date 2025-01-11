package cash.p.terminal.entities.transactionrecords.tron

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.tronkit.models.Transaction

class TronOutgoingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean
) : TronTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
