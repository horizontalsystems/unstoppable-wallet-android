package io.horizontalsystems.bankwallet.entities.transactionrecords.solana

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.models.Transaction

class SolanaOutgoingTransactionRecord(
        transaction: Transaction,
        baseToken: Token,
        source: TransactionSource,
        val to: String?,
        val value: TransactionValue,
        val sentToSelf: Boolean
): SolanaTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
