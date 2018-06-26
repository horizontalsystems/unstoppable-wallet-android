package bitcoin.wallet.core

import bitcoin.wallet.entities.ExchangeRate
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
}

interface INetworkManager {
    fun getUnspentOutputs(): Flowable<List<UnspentOutput>>
    fun getExchangeRates(): Flowable<List<ExchangeRate>>
}

data class WalletData(val words: List<String>)

class DatabaseChangeset<T>(val array: List<T>, val deleted: List<Int> = listOf(), val inserted: List<Int> = listOf(), val updated: List<Int> = listOf()) {

    constructor(array: List<T>, changeset: OrderedCollectionChangeSet?) :
            this(
                    array,
                    changeset?.deletions?.toList() ?: listOf(),
                    changeset?.insertions?.toList() ?: listOf(),
                    changeset?.changes?.toList() ?: listOf()
            )

}
