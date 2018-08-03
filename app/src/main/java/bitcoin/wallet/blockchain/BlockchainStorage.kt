package bitcoin.wallet.blockchain

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import javax.inject.Inject

class BlockchainStorage @Inject constructor(val realm: IDatabaseManager) {

    fun insertOrUpdateTransactions(transactionRecords: List<TransactionRecord>) {
        val databaseManager = Factory.databaseManager
        databaseManager.insertOrUpdateTransactions(transactionRecords)
        databaseManager.close()
    }

    fun updateBlockchainHeight(coinCode: String, height: Long) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateBlockchainHeight(coinCode, height)
        databaseManager.close()
    }

    fun updateBlockchainSyncing(coinCode: String, syncing: Boolean) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateBlockchainSyncing(coinCode, syncing)
        databaseManager.close()
    }

    fun updateBalance(balance: Balance) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateBalance(balance)
        databaseManager.close()
    }

    fun updateExchangeRate(exchangeRate: ExchangeRate) {
        val databaseManager = Factory.databaseManager
        databaseManager.updateExchangeRate(exchangeRate)
        databaseManager.close()
    }

}
