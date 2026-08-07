package io.horizontalsystems.walletkit.entities.transactionrecords.evm

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

open class EvmIncomingTransactionRecord(
    transaction: EvmTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val from: String,
    val value: TransactionValue,
    isSpam: Boolean,
    protected: Boolean,
    customUid: String? = null
) : EvmTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    protected = protected,
    foreignTransaction = true,
    spam = isSpam,
    customUid = customUid
) {

    override val mainValue = value

}
