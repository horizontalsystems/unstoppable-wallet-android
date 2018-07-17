package bitcoin.wallet.blockchain

import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.*

object BlockchainStorage {

    fun insertOrUpdateTransactions(transactionRecords: List<TransactionRecord>) {
        val databaseManager = Factory.databaseManager
        databaseManager.insertOrUpdateTransactions(transactionRecords)
        databaseManager.close()
    }

    fun updateBlockchainInfo(blockchainInfo: BlockchainInfo) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateBlockchainInfo(blockchainInfo)
        databaseManager.close()
    }

    fun updateBalance(balance: Balance) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateBalance(balance)
        databaseManager.close()
    }

    fun updateReceiveAddress(address: ReceiveAddress) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateReceiveAddress(address)
        databaseManager.close()
    }

    fun updateExchangeRate(exchangeRate: ExchangeRate) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateExchangeRate(exchangeRate)
        databaseManager.close()
    }

}
