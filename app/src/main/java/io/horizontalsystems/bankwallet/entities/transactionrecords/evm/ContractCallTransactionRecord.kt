package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCallTransactionRecord(
        fullTransaction: FullTransaction,
        val contractAddress: String,
        val method: String?
): EvmTransactionRecord(fullTransaction) {
    
    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        return TransactionType.ContractCall(contractAddress, method)
    }
}
