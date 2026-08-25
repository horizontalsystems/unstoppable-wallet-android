package io.horizontalsystems.walletkit.entities.transactionrecords.tron

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

open class TronIncomingTransactionRecord(
    transaction: TronTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val from: String,
    val value: TransactionValue,
    spam: Boolean
) : TronTransactionRecord(transaction, baseToken, source, true, spam) {

    override val mainValue = value

}
