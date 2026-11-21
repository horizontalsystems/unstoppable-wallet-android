package io.horizontalsystems.walletkit.core.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Auto-Select starts on only for a user who never touched the Zano node
class ZanoAutoSelectDefaultTest {

    private val default = "https://zano.unstoppable.money:443"

    @Test
    fun onWhenNothingWasEverStored() {
        assertTrue(ZanoNodeManager.autoSelectDefault(storedHost = null, defaultHost = default, hasCustomNodes = false))
    }

    @Test
    fun onWhenTheDefaultNodeIsStored() {
        assertTrue(ZanoNodeManager.autoSelectDefault(storedHost = default, defaultHost = default, hasCustomNodes = false))
    }

    @Test
    fun offWhenAnotherBuiltInNodeWasPicked() {
        assertFalse(ZanoNodeManager.autoSelectDefault(storedHost = "https://node.zano.org:443", defaultHost = default, hasCustomNodes = false))
    }

    @Test
    fun offWhenACustomNodeWasAdded() {
        assertFalse(ZanoNodeManager.autoSelectDefault(storedHost = default, defaultHost = default, hasCustomNodes = true))
        assertFalse(ZanoNodeManager.autoSelectDefault(storedHost = null, defaultHost = default, hasCustomNodes = true))
    }
}
