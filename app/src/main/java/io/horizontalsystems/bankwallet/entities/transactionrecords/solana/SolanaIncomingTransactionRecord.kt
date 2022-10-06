package io.horizontalsystems.bankwallet.entities.transactionrecords.solana

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
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