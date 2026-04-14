package com.quantum.wallet.bankwallet.entities.transactionrecords.evm

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ApproveTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val spender: String,
    val value: TransactionValue,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected) {

    override val mainValue = value

}
