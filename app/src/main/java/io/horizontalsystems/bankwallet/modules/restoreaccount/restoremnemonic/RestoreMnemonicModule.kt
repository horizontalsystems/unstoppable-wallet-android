package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic

import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.hdwalletkit.Language

object RestoreMnemonicModule {

    data class UiState(
        val accountName: String,
        val advancedOptionsEnabled: Boolean,
        val invalidWordRanges: List<IntRange>,
        val error: String?,
        val accountType: AccountType?,
        val wordSuggestions: WordSuggestions?,
        val language: Language,
    )

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)
    data class WordSuggestions(val wordItem: WordItem, val options: List<String>)

}