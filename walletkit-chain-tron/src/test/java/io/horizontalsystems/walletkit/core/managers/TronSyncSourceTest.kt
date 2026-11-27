package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.tronkit.models.RpcSource
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI
import java.net.URL

// Auto-Select hands tron-kit the sources of different providers as one list, so the TronGrid
// API keys must reach TronGrid only, and a source's auth that source only.
class TronSyncSourceTest {

    private fun source(name: String, vararg urls: String, auth: String? = null) =
        EvmSyncSource(id = name, name = name, uris = urls.map { URI(it) }, auth = auth)

    private val tronGrid = source("TronGrid", "https://api.trongrid.io/")
    private val pocket = source("Pocket Network", "https://tron.api.pocket.network")
    private val custom = source("Custom", "https://my.node/tron", auth = "user:secret")

    private val keys = listOf("key-1", "key-2")

    private fun endpoint(url: String, apiKeys: List<String> = emptyList(), auth: String? = null) =
        RpcSource.Endpoint(URL(url), apiKeys, auth)

    @Test
    fun tronGridKeysGoToTronGridOnly() {
        val rpcSource = listOf(tronGrid, pocket).tronRpcSource(Network.Mainnet, keys)

        assertEquals(
            listOf(
                endpoint("https://api.trongrid.io/", apiKeys = keys),
                endpoint("https://tron.api.pocket.network"),
            ),
            rpcSource.endpoints
        )
    }

    @Test
    fun selectedSourceComesFirst() {
        val rpcSource = listOf(pocket, tronGrid).tronRpcSource(Network.Mainnet, keys)

        assertEquals(
            listOf("https://tron.api.pocket.network", "https://api.trongrid.io/"),
            rpcSource.urls.map { it.toString() }
        )
    }

    @Test
    fun customSourceKeepsItsAuthAndGetsNoKeys() {
        val rpcSource = listOf(custom).tronRpcSource(Network.Mainnet, keys)

        assertEquals(listOf(endpoint("https://my.node/tron", auth = "user:secret")), rpcSource.endpoints)
    }

    @Test
    fun tronGridSourceFollowsTheNetwork() {
        val rpcSource = listOf(tronGrid).tronRpcSource(Network.NileTestnet, keys)

        assertEquals(listOf(endpoint("https://nile.trongrid.io/", apiKeys = keys)), rpcSource.endpoints)
    }

    @Test
    fun probeAsksEachSourceForItsLatestBlock() {
        val pinger = TronSourcePinger(keys)

        assertEquals("https://api.trongrid.io/wallet/getnowblock", pinger.request(tronGrid).url.toString())
        assertEquals("https://tron.api.pocket.network/wallet/getnowblock", pinger.request(pocket).url.toString())
        assertEquals("https://my.node/tron/wallet/getnowblock", pinger.request(custom).url.toString())
    }

    @Test
    fun probeSendsEachSourceOnlyItsOwnCredentials() {
        val pinger = TronSourcePinger(keys)

        assertEquals("key-1", pinger.request(tronGrid).header("TRON-PRO-API-KEY"))
        assertNull(pinger.request(tronGrid).header("Authorization"))

        assertNull(pinger.request(pocket).header("TRON-PRO-API-KEY"))
        assertNull(pinger.request(pocket).header("Authorization"))

        assertNull(pinger.request(custom).header("TRON-PRO-API-KEY"))
        assertEquals("Basic " + "user:secret".encodeUtf8().base64(), pinger.request(custom).header("Authorization"))
    }
}
