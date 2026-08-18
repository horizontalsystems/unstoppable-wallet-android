package io.horizontalsystems.bankwallet

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.roi.RoiSelectCoinsPage
import io.horizontalsystems.walletkit.modules.send.SendConfirmationData
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionHavHostPage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Send view models build their confirmation data from state that is filled in asynchronously and
 * never persisted, while the back stack itself is persisted — so after process death the user is
 * restored onto a confirmation screen whose view model came back empty. These cover what the screen
 * does with that.
 */
@RunWith(AndroidJUnit4::class)
class ConfirmationDataRecoveryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun navigation(): Pair<HSNavigation, NavBackStack<HSPage>> {
        val backStack = NavBackStack<HSPage>(RoiSelectCoinsPage, BuySubscriptionHavHostPage)
        return HSNavigation(backStack) to backStack
    }

    @Test
    fun missingDataPopsInsteadOfCrashing() {
        val (navigation, backStack) = navigation()
        var seen: SendConfirmationData? = null

        composeRule.setContent {
            seen = rememberConfirmationData(navigation) { null }
        }
        composeRule.waitForIdle()

        assertNull(seen)
        // The confirmation entry is gone, so the user lands back on the send form.
        assertEquals(1, backStack.size)
    }

    @Test
    fun availableDataIsKept() {
        val (navigation, backStack) = navigation()
        val coin = Coin("uid", "Name", "CODE")
        val token = Token(
            coin = coin,
            blockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null),
            type = TokenType.Native,
            decimals = 8,
        )
        val data = SendConfirmationData(
            amount = java.math.BigDecimal.ONE,
            fee = null,
            address = null,
            contact = null,
            token = token,
            feeCoin = coin,
            memo = null
        )
        var seen: SendConfirmationData? = null

        composeRule.setContent {
            seen = rememberConfirmationData(navigation) { data }
        }
        composeRule.waitForIdle()

        assertEquals(data, seen)
        assertEquals(2, backStack.size)
    }
}
