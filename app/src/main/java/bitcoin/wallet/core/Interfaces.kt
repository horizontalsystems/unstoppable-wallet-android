package bitcoin.wallet.core

import bitcoin.wallet.entities.Coin
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.subjects.PublishSubject

interface ILocalStorage {
    val savedWords : List<String>?
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
    fun getRandomIndexes(count: Int) : List<Int>
}

interface IUnspentOutputProvider {
    val unspentOutputs : List<UnspentOutput>
    val subject: PublishSubject<List<UnspentOutput>>
}

interface IExchangeRateProvider {
    val subject: PublishSubject<HashMap<Coin, Double>>

    fun getExchangeRateForCoin(bitcoin: Coin): Double

}

data class WalletData(val words: List<String>)
