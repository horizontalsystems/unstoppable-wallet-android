package bitcoin.wallet.blockchain

import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.TransactionRecord

object BlockchainStorage {

    val databaseManager = Factory.databaseManager

    fun insertOrUpdateTransaction(transactionRecord: TransactionRecord) {
        databaseManager.insertOrUpdateTransaction(transactionRecord)
    }

    fun updateBlockchainInfo(blockchainInfo: BlockchainInfo) {
        databaseManager.updateBlockchainInfo(blockchainInfo)
    }

}
