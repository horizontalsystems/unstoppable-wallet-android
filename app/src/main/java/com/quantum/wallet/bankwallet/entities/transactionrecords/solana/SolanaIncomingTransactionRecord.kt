package com.quantum.wallet.bankwallet.entities.transactionrecords.solana

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.models.Transaction

class SolanaIncomingTransactionRecord(
        transaction: Transaction,
        baseToken: Token,
        source: TransactionSource,
        val from: String?,
        val value: TransactionValue,
        spam: Boolean = false,
): SolanaTransactionRecord(transaction, baseToken, source, spam) {

    override val mainValue = value

}