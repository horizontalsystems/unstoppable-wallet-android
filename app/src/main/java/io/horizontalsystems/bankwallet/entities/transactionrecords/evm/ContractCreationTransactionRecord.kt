package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class ContractCreationTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source)
