package io.horizontalsystems.walletkit.modules.nearaccount

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearAccountPickerUiStateTest {

    @Test
    fun continuesWithConfirmedAccess() {
        assertTrue(state(selected(accessLost = false)).continueEnabled)
    }

    @Test
    fun blocksLostAccess() {
        assertFalse(state(selected(accessLost = true)).continueEnabled)
    }

    @Test
    fun blocksUncheckedAccess() {
        assertFalse(state(selected(accessLost = null)).continueEnabled)
        assertFalse(state(selected(accessLost = null, checkingAccess = true)).continueEnabled)
    }

    @Test
    fun blocksWithoutSelection() {
        assertFalse(state(selected(accessLost = false).copy(selected = false)).continueEnabled)
    }

    private fun state(vararg items: NearAccountViewItem) = NearAccountPickerUiState(
        searching = false,
        searchFailed = false,
        items = items.toList(),
        publicKey = null,
        result = null,
    )

    private fun selected(accessLost: Boolean?, checkingAccess: Boolean = false) = NearAccountViewItem(
        accountId = "alice.near",
        implicit = false,
        balance = null,
        selected = true,
        accessLost = accessLost,
        checkingAccess = checkingAccess,
        otherFullAccessKeys = false,
        hasContract = false,
    )
}
