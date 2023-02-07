package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.models.Transaction

class SolanaIncomingTransactionRecord(
        transaction: Transaction,
        baseToken: Token,
        source: TransactionSource,
        val from: String?,
        val value: TransactionValue
): SolanaTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}