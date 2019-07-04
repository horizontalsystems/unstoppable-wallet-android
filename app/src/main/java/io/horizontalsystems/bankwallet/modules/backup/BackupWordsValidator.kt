package io.horizontalsystems.bankwallet.modules.backup

import java.util.*

object BackupWordsValidator {
    fun isValid(confirmationWords: HashMap<Int, String>, words: List<String>): Boolean {
        for ((index, word) in confirmationWords) {
            if (words[index - 1] != word.trim()) {
                return false
            }
        }

        return true
    }
}
