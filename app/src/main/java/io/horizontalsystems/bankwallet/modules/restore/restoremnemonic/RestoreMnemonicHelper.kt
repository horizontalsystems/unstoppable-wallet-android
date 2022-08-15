package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicHelper {

    fun getWordSuggestions(
        allItems: List<RestoreMnemonicModule.WordItem>,
        cursorPosition: Int
    ): RestoreMnemonicModule.WordSuggestions? {
        val wordItemWithCursor = allItems.find {
            it.range.contains(cursorPosition - 1)
        } ?: return null

        return RestoreMnemonicModule.WordSuggestions(
            wordItemWithCursor,
            fetchSuggestions(wordItemWithCursor.word, detectLanguages(allItems))
        )
    }

    private fun fetchSuggestions(input: String, languages: List<Language>): List<String> {
        val suggestions = mutableListOf<String>()
        for (lang in languages) {
            val words = WordList.getWords(lang)

            for (word in words) {
                if (word.startsWith(input)) {
                    suggestions.add(word)
                }
            }
        }

        return suggestions.distinct()
    }

    private fun detectLanguages(inputWords: List<RestoreMnemonicModule.WordItem>): List<Language> {
        var languages = Language.values().toList()

        for (wordItem in inputWords) {
            val filteredLanguages = filterLanguages(languages, wordItem.word)
            if (filteredLanguages.isEmpty()) {
                break
            }
            languages = filteredLanguages
        }

        return languages
    }

    private fun filterLanguages(languages: List<Language>, word: String): List<Language> {
        return languages.filter { lang ->
            val words = WordList.getWords(lang)

            words.any { it.startsWith(word) }
        }
    }
}
