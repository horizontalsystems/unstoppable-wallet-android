package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.entities.EvmSyncSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class EvmSyncSourceAutoSelectTest {

    private fun source(name: String, vararg urls: String, ws: Boolean = false, auth: String? = null) =
        EvmSyncSource(id = name, name = name, uris = urls.map { URI(it) }, isWebSocket = ws, auth = auth)

    private val unstoppable = source("Unstoppable", "https://a.example", "https://a2.example")
    private val llama = source("Llama", "https://b.example")
    private val other = source("Other", "https://c.example")
    private val defaults = listOf(unstoppable, llama, other)
    private val custom = source("Custom", "https://my.node", auth = "secret")

    @Test
    fun freshInstallIsUntouched() {
        assertTrue(EvmSyncSourceManager.isUntouched(defaults, storedUrl = null, hasCustomSources = false))
    }

    @Test
    fun storedDefaultIsUntouched() {
        assertTrue(EvmSyncSourceManager.isUntouched(defaults, storedUrl = "https://a.example", hasCustomSources = false))
    }

    @Test
    fun anotherBuiltInSelectedIsTouched() {
        assertFalse(EvmSyncSourceManager.isUntouched(defaults, storedUrl = "https://b.example", hasCustomSources = false))
    }

    @Test
    fun customSourceAddedIsTouched() {
        assertFalse(EvmSyncSourceManager.isUntouched(defaults, storedUrl = null, hasCustomSources = true))
        assertFalse(EvmSyncSourceManager.isUntouched(defaults, storedUrl = "https://a.example", hasCustomSources = true))
    }

    @Test
    fun noDefaultsIsNeverUntouched() {
        assertFalse(EvmSyncSourceManager.isUntouched(emptyList(), storedUrl = null, hasCustomSources = false))
    }

    @Test
    fun autoSelectOffUsesOnlySelected() {
        assertEquals(listOf(llama), EvmSyncSourceManager.failoverOrder(llama, defaults, autoSelect = false))
    }

    @Test
    fun autoSelectPutsSelectedFirstThenOtherBuiltIns() {
        assertEquals(
            listOf(llama, unstoppable, other),
            EvmSyncSourceManager.failoverOrder(llama, defaults, autoSelect = true)
        )
    }

    @Test
    fun customSelectionNeverFallsOverToBuiltIns() {
        assertEquals(listOf(custom), EvmSyncSourceManager.failoverOrder(custom, defaults, autoSelect = true))
    }

    @Test
    fun webSocketAndAuthSourcesAreNotCombined() {
        val ws = source("Ws", "wss://ws.example", ws = true)
        val keyed = source("Keyed", "https://k.example", auth = "key")
        val withOdd = listOf(unstoppable, ws, keyed, llama)

        assertEquals(listOf(unstoppable, llama), EvmSyncSourceManager.failoverOrder(unstoppable, withOdd, autoSelect = true))
        assertEquals(listOf(ws), EvmSyncSourceManager.failoverOrder(ws, withOdd, autoSelect = true))
    }
}
