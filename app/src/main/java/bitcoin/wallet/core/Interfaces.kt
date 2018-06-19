package bitcoin.wallet.core

import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.Flowable

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

interface IDatabaseManager {
    fun getUnspentOutputs() : List<UnspentOutput>
    fun insertUnspentOutputs(values: List<UnspentOutput>)
    fun truncateUnspentOutputs()

    fun getExchangeRates() : List<ExchangeRate>
    fun insertExchangeRates(values: List<ExchangeRate>)
    fun truncateExchangeRates()
}

interface INetworkManager {
    fun getUnspentOutputs(): Flowable<List<UnspentOutput>>
    fun getExchangeRates(): Flowable<List<ExchangeRate>>
}

data class WalletData(val words: List<String>)
