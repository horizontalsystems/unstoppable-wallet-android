package bitcoin.wallet.core

import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.BitcoinUnspentOutput
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCashUnspentOutput
import io.reactivex.Observable
import io.realm.OrderedCollectionChangeSet

interface ILocalStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
    fun clearAll()
}

interface IMnemonic {
    fun generateWords(): List<String>
    fun validateWords(words: List<String>): Boolean
}

interface IWalletDataProvider {
    val walletData: WalletData
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface IDatabaseManager {
    fun getBitcoinUnspentOutputs(): Observable<DatabaseChangeset<BitcoinUnspentOutput>>
    fun getBitcoinCashUnspentOutputs(): Observable<DatabaseChangeset<BitcoinCashUnspentOutput>>
    fun getExchangeRates(): Observable<DatabaseChangeset<ExchangeRate>>
    fun getTransactionRecords(): Observable<DatabaseChangeset<TransactionRecord>>
    fun getBlockchainInfos(): Observable<DatabaseChangeset<BlockchainInfo>>
    fun insertOrUpdateTransaction(transactionRecord: TransactionRecord)
    fun updateBlockchainInfo(blockchainInfo: BlockchainInfo)
}

interface INetworkManager {
    fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String>
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
}

data class WalletData(val words: List<String>)

data class DatabaseChangeset<T>(val array: List<T>, val changeset: CollectionChangeset? = null)

data class CollectionChangeset(val deleted: List<Int> = listOf(), val inserted: List<Int> = listOf(), val updated: List<Int> = listOf()) {

    constructor(changeset: OrderedCollectionChangeSet) :
            this(changeset.deletions.toList(), changeset.insertions.toList(), changeset.changes.toList())

}
