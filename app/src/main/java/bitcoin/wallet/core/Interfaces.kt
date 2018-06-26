package bitcoin.wallet.core

import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.Flowable
import io.reactivex.Observable
import io.realm.OrderedCollectionChangeSet

interface ILocalStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
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
    fun getUnspentOutputs(): Observable<DatabaseChangeset<UnspentOutput>>
    fun getExchangeRates(): Observable<DatabaseChangeset<ExchangeRate>>
    fun getTransactionRecords(): Observable<DatabaseChangeset<TransactionRecord>>
}

interface INetworkManager {
    fun getUnspentOutputs(): Flowable<List<UnspentOutput>>
    fun getExchangeRates(): Flowable<List<ExchangeRate>>
}

data class WalletData(val words: List<String>)

data class DatabaseChangeset<T>(val array: List<T>, val changeset: CollectionChangeset? = null)

data class CollectionChangeset(val deleted: List<Int> = listOf(), val inserted: List<Int> = listOf(), val updated: List<Int> = listOf()) {

    constructor(changeset: OrderedCollectionChangeSet) :
            this(changeset.deletions.toList(), changeset.insertions.toList(), changeset.changes.toList())

}
