package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.PasswordError

class PassphraseValidator {
    private val allowedCharacters = "abcdefghijklmnopqrstuvwxyz0123456789 '\"`&/?!:;.,~*$=+-[](){}<>\\_#@|%"

    fun containsValidCharacters(text: String?): Boolean {
        return text?.all { allowedCharacters.contains(it, ignoreCase = true) } ?: true
    }

    @Throws(PasswordError::class)
    fun validatePassword(password: String) {
            if (password.length < 8
                || !password.contains(Regex("[A-Z]"))
                || !password.contains(Regex("[a-z]"))
                || !password.contains(Regex("[0-9]"))
                || !password.contains(Regex("['\"`&/?!:;,~*\$=+\\-\\[\\](){}<>\\\\_#@|%]"))) {
                throw PasswordError.PasswordInvalid
            }
    }

}
