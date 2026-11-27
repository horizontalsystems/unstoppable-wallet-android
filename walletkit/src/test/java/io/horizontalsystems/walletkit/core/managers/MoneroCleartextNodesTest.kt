package io.horizontalsystems.walletkit.core.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// The node ping goes over plain HTTP unless the port is a TLS one, and the app refuses plain
// HTTP to a host that is not named in the network security config. A built-in node left out
// there shows as unreachable for good.
class MoneroCleartextNodesTest {

    private val tlsPorts = setOf(443, 18443) // as monero-kit's NodeInfo picks the scheme

    @Test
    fun everyBuiltInNodePingedOverHttpMayBeReached() {
        val pingedOverHttp = MoneroNodeManager.builtInNodes
            .map { it.host.substringBeforeLast(':') to it.host.substringAfterLast(':').toInt() }
            .filter { (_, port) -> port !in tlsPorts }
            .map { (host, _) -> host }

        assertTrue("no built-in node is pinged over HTTP: the test reads the list wrong", pingedOverHttp.isNotEmpty())
        assertEquals(emptyList<String>(), pingedOverHttp - cleartextHosts())
    }

    private fun cleartextHosts(): Set<String> {
        val config = File("src/main/res/xml/network_security_config.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config)
        val configs = document.getElementsByTagName("domain-config")

        return (0 until configs.length)
            .map { configs.item(it) as Element }
            .filter { it.getAttribute("cleartextTrafficPermitted") == "true" }
            .flatMap { domains ->
                val list = domains.getElementsByTagName("domain")
                (0 until list.length).map { list.item(it).textContent.trim() }
            }
            .toSet()
    }
}
