package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicHelper {

    fun getWordSuggestions(
        allItems: List<RestoreMnemonicModule.WordItem>,
        cursorPosition: Int
    ): RestoreMnemonicModule.WordSuggestions? {
        val wordItemWithCursor = allItems.find {
            it.range.contains(cursorPosition - 1)
        } ?: return null

        val inputWords = allItems.map { it.word }
        val languages = WordList.detectLanguages(inputWords)
        val suggestions = mutableListOf<String>()
        languages.forEach { language ->
            val wordList = WordList.wordList(language)
            inputWords.forEach { word ->
                suggestions.addAll(wordList.fetchSuggestions(word))
            }
        }

        return RestoreMnemonicModule.WordSuggestions(wordItemWithCursor, suggestions.distinct())
    }

}
