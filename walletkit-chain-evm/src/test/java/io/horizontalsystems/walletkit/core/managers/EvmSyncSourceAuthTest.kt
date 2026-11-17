package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import okhttp3.Credentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

// Auto-Select combines sources of different providers into one list, so a source's auth must
// reach that source's URLs and no other provider's.
class EvmSyncSourceAuthTest {

    private fun source(name: String, vararg urls: String, auth: String? = null) =
        EvmSyncSource(id = name, name = name, uris = urls.map { URI(it) }, auth = auth)

    private val open = source("Open", "https://open.example")
    private val keyed = source("Keyed", "https://k1.example", "https://k2.example", auth = "keyed-secret")
    private val other = source("Other", "https://other.example", auth = "other-secret")

    private fun endpoint(url: String, auth: String? = null) = RpcSource.Endpoint(URI(url), auth)

    @Test
    fun combinedSourceKeepsEachAuthWithItsOwnUrls() {
        val rpcSource = listOf(keyed, open, other).failoverRpcSource as RpcSource.Http

        assertEquals(
            listOf(
                endpoint("https://k1.example", "keyed-secret"),
                endpoint("https://k2.example", "keyed-secret"),
                endpoint("https://open.example"),
                endpoint("https://other.example", "other-secret"),
            ),
            rpcSource.endpoints
        )
    }

    @Test
    fun selectedSourceWithAuthDoesNotShareItWithFallbacks() {
        val rpcSource = listOf(keyed, open).failoverRpcSource as RpcSource.Http

        assertEquals(endpoint("https://open.example"), rpcSource.endpoints.last())
    }

    @Test
    fun sameUrlInTwoSourcesKeepsEachSourcesAuth() {
        val mirror = source("Mirror", "https://k1.example", auth = "mirror-secret")

        val rpcSource = listOf(keyed, mirror).failoverRpcSource as RpcSource.Http

        assertEquals(
            listOf(
                endpoint("https://k1.example", "keyed-secret"),
                endpoint("https://k2.example", "keyed-secret"),
                endpoint("https://k1.example", "mirror-secret"),
            ),
            rpcSource.endpoints
        )
    }

    @Test
    fun singleSourceKeepsItsAuth() {
        val rpcSource = listOf(keyed).failoverRpcSource as RpcSource.Http

        assertEquals(
            listOf(endpoint("https://k1.example", "keyed-secret"), endpoint("https://k2.example", "keyed-secret")),
            rpcSource.endpoints
        )
    }

    @Test
    fun probeSendsTheSourceAuth() {
        assertEquals(
            Credentials.basic("", "keyed-secret"),
            EvmSourcePinger.request(keyed).header("Authorization")
        )
    }

    @Test
    fun probeOfASourceWithoutAuthSendsNoHeader() {
        assertNull(EvmSourcePinger.request(open).header("Authorization"))
    }
}
