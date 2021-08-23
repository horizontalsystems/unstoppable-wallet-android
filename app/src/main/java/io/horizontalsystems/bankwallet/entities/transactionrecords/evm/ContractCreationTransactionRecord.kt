package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCreationTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source)
