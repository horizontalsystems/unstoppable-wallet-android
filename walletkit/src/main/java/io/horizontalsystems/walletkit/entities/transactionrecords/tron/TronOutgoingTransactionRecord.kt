package io.horizontalsystems.walletkit.entities.transactionrecords.tron

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

open class TronOutgoingTransactionRecord(
    transaction: TronTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean
) : TronTransactionRecord(transaction, baseToken, source) {

    override val mainValue = value

}
