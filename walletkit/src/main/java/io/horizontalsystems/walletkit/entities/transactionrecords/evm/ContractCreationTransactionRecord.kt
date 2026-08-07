package io.horizontalsystems.walletkit.entities.transactionrecords.evm

import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

class ContractCreationTransactionRecord(
    transaction: EvmTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source, protected)
