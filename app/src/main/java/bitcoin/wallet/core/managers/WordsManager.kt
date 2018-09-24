package bitcoin.wallet.core.managers

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.kit.hdwallet.Mnemonic

class WordsManager(private val storage: ILocalStorage) {

    fun savedWords(): List<String>? {
        return storage.savedWords
    }

    fun createWords(): List<String> {
        val generatedWords = Mnemonic().generate()
        storage.saveWords(generatedWords)
        return generatedWords
    }

    fun restore(words: List<String>) {
        Mnemonic().validate(words)
        storage.saveWords(words)
    }

}
