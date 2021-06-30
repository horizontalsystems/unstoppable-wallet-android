package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCreationTransactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord(fullTransaction) {

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        return TransactionType.ContractCreation
    }
}
