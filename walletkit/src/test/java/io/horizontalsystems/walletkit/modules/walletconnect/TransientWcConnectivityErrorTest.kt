package io.horizontalsystems.walletkit.modules.walletconnect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The WC/Reown relay reports transient reconnect failures through the global error delegate (e.g.
 * on app resume, before the network is ready). Those recover on their own and shouldn't surface a
 * HUD. These tests pin the classifier so it stays scoped to the known connectivity signatures and
 * doesn't swallow genuine, actionable errors that merely mention a network word.
 */
class TransientWcConnectivityErrorTest {

    @Test
    fun batchSubscribeErrorIsTransient() {
        // The reported symptom, plus the general "Batch subscribe error: ..." SDK form.
        assertTrue(transient("Batch subscribe connectivity error. Check your internet connection"))
        assertTrue(transient("Batch subscribe error: request timeout"))
    }

    @Test
    fun noConnectionAvailableIsTransient() {
        // NoRelayConnectionException delivered via the global delegate on reconnect.
        assertTrue(transient("No connection available"))
    }

    @Test
    fun noInternetConnectionIsTransient() {
        assertTrue(transient("Connection error: Please check your Internet connection"))
    }

    @Test
    fun classificationIsCaseInsensitive() {
        assertTrue(transient("BATCH SUBSCRIBE ERROR: timeout"))
    }

    @Test
    fun nullMessageIsNotTransient() {
        assertFalse(isTransientWcConnectivityError(Throwable()))
    }

    @Test
    fun genuineErrorsWithNetworkWordingAreNotTransient() {
        // Near-matches that share a single loose word must remain actionable, not be suppressed.
        assertFalse(transient("Failed to sign transaction: connectivity lost"))
        assertFalse(transient("Session approve error: user rejected"))
        assertFalse(transient("Publish error: invalid request"))
        assertFalse(transient("Subscribe error: topic already subscribed"))
    }

    private fun transient(message: String) = isTransientWcConnectivityError(Throwable(message))
}
