package com.quantum.wallet.bankwallet.entities.transactionrecords.evm

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class EvmOutgoingTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val to: String,
    val value: TransactionValue,
    val sentToSelf: Boolean,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected) {

    override val mainValue = value

}
