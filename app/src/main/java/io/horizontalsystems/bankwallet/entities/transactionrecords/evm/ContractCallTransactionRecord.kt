package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCallTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    val contractAddress: String,
    val method: String?
) : EvmTransactionRecord(fullTransaction, baseCoin) {

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        return TransactionType.ContractCall(contractAddress, method)
    }
}
