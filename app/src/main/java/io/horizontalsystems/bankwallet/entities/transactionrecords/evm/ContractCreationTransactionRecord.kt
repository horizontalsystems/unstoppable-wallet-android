package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCreationTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin
) : EvmTransactionRecord(fullTransaction, baseCoin) {

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        return TransactionType.ContractCreation
    }
}
