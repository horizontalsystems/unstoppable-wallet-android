package io.horizontalsystems.core.entities.transactionrecords.evm

import io.horizontalsystems.core.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ContractCreationTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected)
