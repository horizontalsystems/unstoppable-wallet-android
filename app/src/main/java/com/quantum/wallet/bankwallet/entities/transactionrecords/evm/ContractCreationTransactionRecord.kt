package com.quantum.wallet.bankwallet.entities.transactionrecords.evm

import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ContractCreationTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected)
