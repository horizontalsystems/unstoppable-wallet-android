package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class EvmIncomingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val from: String,
    val value: TransactionValue,
    isSpam: Boolean
) : EvmTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    foreignTransaction = true,
    spam = isSpam
) {

    override val mainValue = value

}
