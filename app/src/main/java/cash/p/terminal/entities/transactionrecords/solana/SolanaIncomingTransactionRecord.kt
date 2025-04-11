package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.solanakit.models.Transaction

class SolanaIncomingTransactionRecord(
    transaction: Transaction,
    val baseToken: Token,
    source: TransactionSource,
    val from: String?,
    val value: TransactionValue,
    val isSpam: Boolean
) : SolanaTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    spam = isSpam,
    transactionRecordType = TransactionRecordType.SOLANA_INCOMING
) {

    override val mainValue = value

}