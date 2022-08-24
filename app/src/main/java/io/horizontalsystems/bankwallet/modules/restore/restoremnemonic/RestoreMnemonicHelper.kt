package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicHelper {

    fun getWordSuggestions(
        allItems: List<RestoreMnemonicModule.WordItem>,
        cursorPosition: Int,
        language: Language
    ): RestoreMnemonicModule.WordSuggestions? {
        val wordItemWithCursor = allItems.find {
            it.range.contains(cursorPosition - 1)
        } ?: return null

        return RestoreMnemonicModule.WordSuggestions(
            wordItemWithCursor,
            WordList.wordList(language).fetchSuggestions(wordItemWithCursor.word)
        )
    }
}
