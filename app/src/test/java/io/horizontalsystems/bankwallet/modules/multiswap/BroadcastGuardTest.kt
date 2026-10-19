package io.horizontalsystems.bankwallet.modules.multiswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * UI tests in E2E builds use mainnet wallets, so every send service must refuse to broadcast
 * there. The guard is a call at the top of each override rather than a wrapper, which a new
 * chain can forget; this test finds the overrides in the sources and checks each one.
 */
class BroadcastGuardTest {

    private val override = Regex("""override suspend fun sendTransaction\(mevProtectionEnabled: Boolean\)[^{]*\{""")

    @Test
    fun everySendServiceChecksTheGuardFirst() {
        val repoRoot = File("..")
        val sources = repoRoot.listFiles { f -> f.isDirectory && f.name.startsWith("walletkit") }!!
            .map { File(it, "src/main/java") }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walk().filter { it.extension == "kt" }.toList() }

        val services = sources.filter { override.containsMatchIn(it.readText()) }
        assertTrue("No send services found under $repoRoot", services.size >= 10)

        val unguarded = services.filter { file ->
            val text = file.readText()
            override.findAll(text).any { match ->
                val body = text.substring(match.range.last + 1).trimStart()
                !body.startsWith("checkBroadcastAllowed()")
            }
        }.map { it.name }

        assertEquals(emptyList<String>(), unguarded)
    }
}
