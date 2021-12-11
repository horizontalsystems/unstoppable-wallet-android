package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class ApproveTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    val value: TransactionValue,
    val spender: String,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {

    override val mainValue = value

}
