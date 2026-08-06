package io.horizontalsystems.walletkit.entities.transactionrecords.solana

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

class SolanaIncomingTransactionRecord(
        transaction: SolanaTransactionInfo,
        baseToken: Token,
        source: TransactionSource,
        val from: String?,
        val value: TransactionValue,
        spam: Boolean = false,
): SolanaTransactionRecord(transaction, baseToken, source, spam) {

    override val mainValue = value

}