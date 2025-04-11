package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.solanakit.models.Transaction

class SolanaOutgoingTransactionRecord(
    transaction: Transaction,
    val baseToken: Token,
    source: TransactionSource,
    val to: String?,
    val value: TransactionValue,
    val sentToSelf: Boolean
) : SolanaTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    transactionRecordType = TransactionRecordType.SOLANA_OUTGOING
) {

    override val mainValue = value

}
