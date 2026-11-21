package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.testutil.FakeAccountManager
import io.horizontalsystems.walletkit.testutil.FakeWalletStorage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class WalletManagerUpdatesTest {

    private val account = Account(
        id = "account",
        name = "Account",
        type = AccountType.Mnemonic(List(12) { "word" }, ""),
        origin = AccountOrigin.Created,
        level = 0,
    )

    private val ethereum = Wallet(
        Token(Coin("ethereum", "Ethereum", "ETH"), Blockchain(BlockchainType.Ethereum, "Ethereum", null), TokenType.Native, 18),
        account,
    )

    // A chain's adapters are rebuilt by removing its wallets and adding them back
    // (reloadWallets). AdapterManager only drops the old adapters, bound to the kit
    // that was just stopped, when it sees the list without them, so a collector that
    // is busy while both updates are emitted must still receive both.
    @Test
    fun busyCollectorReceivesBothUpdatesOfAReload() = runBlocking {
        val walletManager = WalletManager(FakeAccountManager(account), FakeWalletStorage())
        walletManager.save(listOf(ethereum))

        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            val received = async(dispatcher, start = CoroutineStart.UNDISPATCHED) {
                walletManager.activeWalletsUpdatedFlow.take(2).toList()
            }

            // Keep the collector's thread busy while both updates are emitted.
            val release = CountDownLatch(1)
            executor.execute { release.await() }

            walletManager.delete(listOf(ethereum))
            walletManager.save(listOf(ethereum))
            release.countDown()

            val updates = withTimeout(5_000) { received.await() }
            assertEquals(listOf(emptyList(), listOf(ethereum)), updates)
        } finally {
            dispatcher.close()
        }
    }

    // Startup work that depends on which chains are in use (the Auto-Select re-pick) runs
    // while the wallets are still being read, when the list is empty for every chain.
    @Test
    fun walletsAreLoadedOnlyOnceTheActiveAccountIsKnown() = runBlocking {
        val accountState = MutableStateFlow<ActiveAccountState>(ActiveAccountState.NotLoaded)
        val storage = FakeWalletStorage().apply { save(listOf(ethereum)) }
        val walletManager = WalletManager(FakeAccountManager(account, accountState), storage)

        assertNull(withTimeoutOrNull(200) { walletManager.awaitLoaded() })
        assertEquals(emptyList<Wallet>(), walletManager.activeWallets)

        accountState.value = ActiveAccountState.ActiveAccount(account)

        withTimeout(5_000) { walletManager.awaitLoaded() }
        assertEquals(listOf(ethereum), walletManager.activeWallets)
    }
}
