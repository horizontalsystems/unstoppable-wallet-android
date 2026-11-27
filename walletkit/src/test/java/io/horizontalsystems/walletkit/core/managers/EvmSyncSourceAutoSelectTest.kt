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
        assertTrue(EvmSyncSourceAutoSelect.isUntouched(defaults, storedUrl = null, hasCustomSources = false))
    }

    @Test
    fun storedDefaultIsUntouched() {
        assertTrue(EvmSyncSourceAutoSelect.isUntouched(defaults, storedUrl = "https://a.example", hasCustomSources = false))
    }

    @Test
    fun anotherBuiltInSelectedIsTouched() {
        assertFalse(EvmSyncSourceAutoSelect.isUntouched(defaults, storedUrl = "https://b.example", hasCustomSources = false))
    }

    @Test
    fun customSourceAddedIsTouched() {
        assertFalse(EvmSyncSourceAutoSelect.isUntouched(defaults, storedUrl = null, hasCustomSources = true))
        assertFalse(EvmSyncSourceAutoSelect.isUntouched(defaults, storedUrl = "https://a.example", hasCustomSources = true))
    }

    @Test
    fun noDefaultsIsNeverUntouched() {
        assertFalse(EvmSyncSourceAutoSelect.isUntouched(emptyList(), storedUrl = null, hasCustomSources = false))
    }

    @Test
    fun autoSelectOffUsesOnlySelected() {
        assertEquals(listOf(llama), EvmSyncSourceAutoSelect.failoverOrder(llama, defaults, autoSelect = false))
    }

    @Test
    fun autoSelectPutsSelectedFirstThenOtherBuiltIns() {
        assertEquals(
            listOf(llama, unstoppable, other),
            EvmSyncSourceAutoSelect.failoverOrder(llama, defaults, autoSelect = true)
        )
    }

    @Test
    fun customSelectionNeverFallsOverToBuiltIns() {
        assertEquals(listOf(custom), EvmSyncSourceAutoSelect.failoverOrder(custom, defaults, autoSelect = true))
    }

    @Test
    fun webSocketSourcesAreNotCombined() {
        val ws = source("Ws", "wss://ws.example", ws = true)
        val withWs = listOf(unstoppable, ws, llama)

        assertEquals(listOf(unstoppable, llama), EvmSyncSourceAutoSelect.failoverOrder(unstoppable, withWs, autoSelect = true))
        assertEquals(listOf(ws), EvmSyncSourceAutoSelect.failoverOrder(ws, withWs, autoSelect = true))
    }

    @Test
    fun builtInSourceWithAuthTakesPartLikeAnyOther() {
        val keyed = source("Keyed", "https://k.example", auth = "key")
        val withKeyed = listOf(unstoppable, keyed, llama)

        assertEquals(
            listOf(unstoppable, keyed, llama),
            EvmSyncSourceAutoSelect.failoverOrder(unstoppable, withKeyed, autoSelect = true)
        )
        assertEquals(
            listOf(keyed, unstoppable, llama),
            EvmSyncSourceAutoSelect.failoverOrder(keyed, withKeyed, autoSelect = true)
        )
    }
}
