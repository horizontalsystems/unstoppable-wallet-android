package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class EvmIncomingTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    val value: TransactionValue,
    val from: String,
    override val foreignTransaction: Boolean = false,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {

    override val mainValue = value

}
