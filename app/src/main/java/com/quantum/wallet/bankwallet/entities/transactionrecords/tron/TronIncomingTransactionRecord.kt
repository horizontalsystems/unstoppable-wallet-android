package com.quantum.wallet.bankwallet.entities.transactionrecords.tron

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.models.Transaction

class TronIncomingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val from: String,
    val value: TransactionValue,
    spam: Boolean
) : TronTransactionRecord(transaction, baseToken, source, true, spam) {

    override val mainValue = value

}
