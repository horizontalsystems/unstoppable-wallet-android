package cash.p.terminal.modules.restoreaccount.restoremnemonic

import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.hdwalletkit.Language

object RestoreMnemonicModule {
    data class UiState(
        val passphraseEnabled: Boolean,
        val passphraseError: String?,
        val invalidWordRanges: List<IntRange>,
        val error: String?,
        val errorHeight: String?,
        val height: String,
        val isMoneroMnemonic: Boolean,
        val accountType: AccountType?,
        val wordSuggestions: WordSuggestions?,
        val language: Language,
    )

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)
    data class WordSuggestions(val wordItem: WordItem, val options: List<String>)

}
