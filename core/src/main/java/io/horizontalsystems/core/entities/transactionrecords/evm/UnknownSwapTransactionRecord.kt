package io.horizontalsystems.core.entities.transactionrecords.evm

import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class UnknownSwapTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val exchangeAddress: String,
    val valueIn: TransactionValue?,
    val valueOut: TransactionValue?,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected)
