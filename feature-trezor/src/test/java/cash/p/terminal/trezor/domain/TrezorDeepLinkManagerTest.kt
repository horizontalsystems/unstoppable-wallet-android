package cash.p.terminal.trezor.domain

import android.content.Context
import cash.p.terminal.trezor.domain.model.TrezorMethod
import cash.p.terminal.trezor.domain.model.TrezorResponse
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrezorDeepLinkManagerTest {

    @Test
    fun buildUrl_btcGetAddress_correctUrl() {
        val params = JsonObject(
            mapOf(
                "coin" to JsonPrimitive("btc"),
                "path" to JsonPrimitive("m/84'/0'/0'/0/0")
            )
        )
        val callback = "pcash-trezor://trezor-result?requestToken=test-token-123"

        val url = TrezorDeepLinkManager.buildUrl(TrezorMethod.BtcGetAddress, params, callback)

        assertTrue(url.startsWith("https://connect.trezor.io/9/deeplink/1/"))
        assertTrue(url.contains("method=getAddress"))
        assertTrue(url.contains("callback="))
        assertTrue(url.contains("appName=P.CASH"))
    }

    @Test
    fun buildUrl_ethSignTransaction_usesEthereumPrefix() {
        val params = JsonObject(emptyMap())
        val callback = "pcash-trezor://trezor-result?requestToken=test-token"

        val url = TrezorDeepLinkManager.buildUrl(TrezorMethod.EthSignTransaction, params, callback)

        assertTrue(url.contains("method=ethereumSignTransaction"))
    }

    @Test
    fun buildUrl_solGetPublicKey_usesSolanaPrefix() {
        val params = JsonObject(emptyMap())
        val callback = "pcash-trezor://trezor-result?requestToken=test-token"

        val url = TrezorDeepLinkManager.buildUrl(TrezorMethod.SolGetPublicKey, params, callback)

        assertTrue(url.contains("method=solanaGetPublicKey"))
    }

    @Test
    fun onCallbackReceived_wrongToken_ignored() {
        val manager = createManager()

        // No pending request — wrong token should be silently ignored
        manager.onCallbackReceived("wrong-token", """{"success":true,"payload":null}""")
    }

    @Test
    fun onCallbackReceived_matchingToken_completesDeferred() {
        val manager = createManager()
        val deferred = CompletableDeferred<TrezorResponse>()
        manager.setPendingForTest("known-token", deferred)

        manager.onCallbackReceived("known-token", """{"success":true,"payload":null}""")

        assertTrue(deferred.isCompleted)
    }

    @Test
    fun onCallbackReceived_wrongTokenWithPending_ignored() {
        val manager = createManager()
        val deferred = CompletableDeferred<TrezorResponse>()
        manager.setPendingForTest("known-token", deferred)

        manager.onCallbackReceived("attacker-token", """{"success":true,"payload":null}""")

        assertFalse(deferred.isCompleted)
    }

    @Test
    fun onCallbackReceived_replayToken_ignored() {
        val manager = createManager()
        val deferred = CompletableDeferred<TrezorResponse>()
        manager.setPendingForTest("known-token", deferred)

        // First callback completes the deferred and invalidates the token
        manager.onCallbackReceived("known-token", """{"success":true,"payload":null}""")
        assertTrue(deferred.isCompleted)

        // Replay with same token — should be ignored (token already invalidated)
        // No crash, no effect
        manager.onCallbackReceived("known-token", """{"success":false,"payload":null}""")
    }

    @Test
    fun trezorMethodValues_matchConnectApiSpec() {
        assertEquals("authorizeCoinjoin", TrezorMethod.BtcAuthorizeCoinjoin.value)
        assertEquals("composeTransaction", TrezorMethod.BtcComposeTransaction.value)
        assertEquals("getAccountInfo", TrezorMethod.BtcGetAccountInfo.value)
        assertEquals("getAddress", TrezorMethod.BtcGetAddress.value)
        assertEquals("getPublicKey", TrezorMethod.BtcGetPublicKey.value)
        assertEquals("pushTransaction", TrezorMethod.BtcPushTransaction.value)
        assertEquals("signMessage", TrezorMethod.BtcSignMessage.value)
        assertEquals("signTransaction", TrezorMethod.BtcSignTransaction.value)
        assertEquals("verifyMessage", TrezorMethod.BtcVerifyMessage.value)
        assertEquals("ethereumGetAddress", TrezorMethod.EthGetAddress.value)
        assertEquals("ethereumGetPublicKey", TrezorMethod.EthGetPublicKey.value)
        assertEquals("ethereumSignTransaction", TrezorMethod.EthSignTransaction.value)
        assertEquals("ethereumVerifyMessage", TrezorMethod.EthVerifyMessage.value)
        assertEquals("solanaGetPublicKey", TrezorMethod.SolGetPublicKey.value)
        assertEquals("solanaComposeTransaction", TrezorMethod.SolComposeTransaction.value)
        assertEquals("stellarGetAddress", TrezorMethod.XlmGetAddress.value)
        assertEquals("tronGetAddress", TrezorMethod.TrxGetAddress.value)
        assertEquals("moneroGetAddress", TrezorMethod.XmrGetAddress.value)
        assertEquals("moneroGetWatchKey", TrezorMethod.XmrGetWatchKey.value)
        assertEquals("moneroKeyImageSync", TrezorMethod.XmrKeyImageSync.value)
        assertEquals("cardanoGetAddress", TrezorMethod.AdaGetAddress.value)
        assertEquals("cardanoGetPublicKey", TrezorMethod.AdaGetPublicKey.value)
        assertEquals("cardanoSignTransaction", TrezorMethod.AdaSignTransaction.value)
        assertEquals("binanceGetAddress", TrezorMethod.BnbGetAddress.value)
        assertEquals("rippleGetAddress", TrezorMethod.XrpGetAddress.value)
        assertEquals("tezosGetAddress", TrezorMethod.XtzGetAddress.value)
        assertEquals("tezosGetPublicKey", TrezorMethod.XtzGetPublicKey.value)
        assertEquals("eosGetPublicKey", TrezorMethod.EosGetPublicKey.value)
        assertEquals("nemGetAddress", TrezorMethod.NemGetAddress.value)
    }

    private fun createManager(): TrezorDeepLinkManager {
        val context: Context = mockk(relaxed = true)
        val backgroundManager: BackgroundManager = mockk(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(BackgroundManagerState.EnterForeground)
        }
        return TrezorDeepLinkManager(context, backgroundManager)
    }
}
