package bitcoin.wallet.core

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

data class WalletData(val words: List<String>)
