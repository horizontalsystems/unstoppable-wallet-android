package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager.ZanoNode
import org.junit.Assert.assertEquals
import org.junit.Test

// Auto-Select picks from the built-in nodes and from the custom nodes it can probe
class ZanoAutoSelectCandidatesTest {

    private val builtIn = listOf(
        ZanoNode("https://zano.unstoppable.money:443", "zano.unstoppable.money"),
        ZanoNode("https://node.zano.org:443", "node.zano.org"),
    )

    @Test
    fun onlyTheBuiltInNodesWhenNoCustomNodeWasAdded() {
        assertEquals(builtIn, ZanoNodeManager.autoSelectCandidates(builtIn, emptyList()))
    }

    @Test
    fun aCustomNodeWithAnHttpsAddressTakesPart() {
        val custom = ZanoNode("https://my.node.example:443", "https://my.node.example:443")

        assertEquals(builtIn + custom, ZanoNodeManager.autoSelectCandidates(builtIn, listOf(custom)))
    }

    @Test
    fun aCustomNodeWithAPlainHttpAddressIsLeftOut() {
        val plain = ZanoNode("http://192.168.1.10:11211", "http://192.168.1.10:11211")
        val secure = ZanoNode("HTTPS://my.node.example", "HTTPS://my.node.example")

        assertEquals(builtIn + secure, ZanoNodeManager.autoSelectCandidates(builtIn, listOf(plain, secure)))
    }

    @Test
    fun aBuiltInNodeTakesPartWhateverItsAddress() {
        val local = ZanoNode("http://192.168.0.2:11211", "local")

        assertEquals(listOf(local) + builtIn, ZanoNodeManager.autoSelectCandidates(listOf(local) + builtIn, emptyList()))
    }
}
