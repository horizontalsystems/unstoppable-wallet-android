package io.horizontalsystems.walletkit.entities.transactionrecords.evm

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

open class EvmOutgoingTransactionRecord(
    transaction: EvmTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean,
    protected: Boolean,
    customUid: String? = null
) : EvmTransactionRecord(transaction, baseToken, source, protected, customUid = customUid) {

    override val mainValue = value

}
