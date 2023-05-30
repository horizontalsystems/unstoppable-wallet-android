package io.horizontalsystems.bankwallet.entities.transactionrecords.tron

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.models.Transaction

class TronIncomingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val from: String,
    val value: TransactionValue
) : TronTransactionRecord(transaction, baseToken, source, true) {

    override val mainValue = value

}
