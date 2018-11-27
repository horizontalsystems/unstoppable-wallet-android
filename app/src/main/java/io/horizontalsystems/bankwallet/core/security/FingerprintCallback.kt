package io.horizontalsystems.bankwallet.core.security

interface FingerprintCallback {
    fun onAuthenticated()

    fun onAuthenticationHelp(helpString: CharSequence?)

    fun onAuthenticationFailed()

    fun onAuthenticationError(errMsgId: Int, errString: CharSequence?)
}
