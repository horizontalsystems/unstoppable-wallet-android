package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class EvmOutgoingTransactionRecord(
    transaction: Transaction,
    baseCoin: PlatformCoin,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean
) : EvmTransactionRecord(transaction, baseCoin, source) {

    override val mainValue = value

}
