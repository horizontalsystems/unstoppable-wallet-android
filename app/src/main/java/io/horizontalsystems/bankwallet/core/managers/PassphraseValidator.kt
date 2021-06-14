package io.horizontalsystems.bankwallet.core.managers

class PassphraseValidator {
    private val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 '\"`&/?!:;.,~*$=+-[](){}<>\\_#@|%"

    fun validate(text: String?): Boolean {
        return text?.all { allowedCharacters.contains(it, ignoreCase = true) } ?: true
    }

}
