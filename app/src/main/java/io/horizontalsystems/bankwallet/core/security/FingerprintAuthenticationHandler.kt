package io.horizontalsystems.bankwallet.core.security

import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import io.horizontalsystems.bankwallet.core.App


class FingerprintAuthenticationHandler(
        private var callback: FingerprintCallback?,
        private val cryptoObject: FingerprintManagerCompat.CryptoObject) : FingerprintManagerCompat.AuthenticationCallback() {

    private val fingerprintManager: FingerprintManagerCompat = FingerprintManagerCompat.from(App.instance)
    private var cancellationSignal: CancellationSignal? = null

    fun releaseFingerprintCallback() {
        callback = null
    }

    /**
     * Start listening for fingerprint authentication.
     */
    fun startListening() {
        cancellationSignal = CancellationSignal()
        fingerprintManager.authenticate(cryptoObject, 0, cancellationSignal, this, null)
    }

    /**
     * Stop listening for fingerprint authentication.
     */
    fun stopListening() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
        super.onAuthenticationError(errMsgId, errString)
        callback?.onAuthenticationError(errMsgId, errString)
    }

    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
        super.onAuthenticationSucceeded(result)
        callback?.onAuthenticated()
    }

    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
        super.onAuthenticationHelp(helpMsgId, helpString)
        callback?.onAuthenticationHelp(helpString)
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        callback?.onAuthenticationFailed()
    }
}
