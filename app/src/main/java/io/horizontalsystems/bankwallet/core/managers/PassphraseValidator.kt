package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.PasswordError

class PassphraseValidator {
    private val allowedCharacters = "abcdefghijklmnopqrstuvwxyz0123456789 '\"`&/?!:;.,~*$=+-[](){}<>\\_#@|%"
    private val pattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*['\"`&/?!:;,~*\$=+\\-\\[\\](){}<>\\\\_#@|%]).{8,}\$")


    fun containsValidCharacters(text: String?): Boolean {
        return text?.all { allowedCharacters.contains(it, ignoreCase = true) } ?: true
    }

    @Throws(PasswordError::class)
    fun validatePassword(password: String) {
        if (!pattern.matches(password)) {
            if (password.length < 8) {
                throw PasswordError.PasswordTooShort
            }
            if (!password.contains(Regex("[A-Z]"))) {
                throw PasswordError.PasswordWithoutUppercase
            }
            if (!password.contains(Regex("[a-z]"))) {
                throw PasswordError.PasswordWithoutLowercase
            }
            if (!password.contains(Regex("[0-9]"))) {
                throw PasswordError.PasswordWithoutNumber
            }
            if (!password.contains(Regex("['\"`&/?!:;,~*\$=+\\-\\[\\](){}<>\\\\_#@|%]"))) {
                throw PasswordError.PasswordWithoutSymbol
            }
            throw PasswordError.PasswordContainsInvalidCharacters
        }
    }

}
