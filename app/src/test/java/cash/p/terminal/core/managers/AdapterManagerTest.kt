package cash.p.terminal.core.managers

import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdapterManagerTest {

    private lateinit var walletManager: IWalletManager
    private lateinit var adapterFactory: AdapterFactory
    private lateinit var activeWalletsFlow: MutableStateFlow<List<Wallet>>
    private lateinit var adapterManager: AdapterManager

    @Before
    fun setUp() {
        activeWalletsFlow = MutableStateFlow(emptyList())

        walletManager = mockk(relaxed = true) {
            every { activeWallets } returns emptyList()
            every { activeWalletsFlow } returns this@AdapterManagerTest.activeWalletsFlow
        }

        adapterFactory = mockk(relaxed = true)

        adapterManager = AdapterManager(
            walletManager,
            adapterFactory,
            btcBlockchainManager = mockk(relaxed = true) {
                every { restoreModeUpdatedObservable } returns Observable.never()
            },
            evmBlockchainManager = mockk(relaxed = true) {
                every { allBlockchains } returns emptyList()
            },
            solanaKitManager = mockk(relaxed = true) {
                every { kitStoppedObservable } returns Observable.never()
            },
            tronKitManager = mockk(relaxed = true),
            tonKitManager = mockk(relaxed = true),
            moneroKitManager = mockk(relaxed = true) {
                every { kitStoppedObservable } returns Observable.never()
            },
            stellarKitManager = mockk(relaxed = true),
            pendingBalanceCalculator = mockk(relaxed = true),
            fallbackAddressProvider = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        // Cancel the internal coroutineScope to prevent leaking coroutines between tests.
        // AdapterManager doesn't expose a close/destroy method, so we use reflection.
        val scopeField = AdapterManager::class.java.getDeclaredField("coroutineScope")
        scopeField.isAccessible = true
        (scopeField.get(adapterManager) as CoroutineScope).cancel()
        adapterManager.quit()
    }

    /**
     * Verifies the fix from commit 69888376b:
     * Old adapters must be stopped BEFORE new ones are created.
     * This is critical for Zcash SDK which forbids creating a new Synchronizer
     * while another one with the same alias is still active.
     */
    @Test
    fun initAdapters_stopsOldAdaptersBeforeCreatingNew() {
        val oldWallet: Wallet = mockk(relaxed = true)
        val newWallet: Wallet = mockk(relaxed = true)
        val oldAdapter: IAdapter = mockk(relaxed = true)
        val newAdapter: IAdapter = mockk(relaxed = true)

        val callOrder = Collections.synchronizedList(mutableListOf<String>())
        val oldAdapterReady = CountDownLatch(1)
        val newAdapterReady = CountDownLatch(1)

        coEvery { adapterFactory.getAdapterOrNull(oldWallet, any()) } coAnswers {
            oldAdapter.also { oldAdapterReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(newWallet, any()) } coAnswers {
            callOrder.add("getAdapterOrNull(newWallet)")
            newAdapter.also { newAdapterReady.countDown() }
        }
        every { oldAdapter.stop() } answers {
            callOrder.add("oldAdapter.stop()")
        }

        activeWalletsFlow.value = listOf(oldWallet)
        adapterManager.startAdapterManager()
        assertTrue("Old adapter was never created", oldAdapterReady.await(5, TimeUnit.SECONDS))

        activeWalletsFlow.value = listOf(newWallet)
        assertTrue("New adapter was never created", newAdapterReady.await(5, TimeUnit.SECONDS))

        val stopIndex = callOrder.indexOf("oldAdapter.stop()")
        val createIndex = callOrder.indexOf("getAdapterOrNull(newWallet)")
        assertTrue("oldAdapter.stop() was never called, calls: $callOrder", stopIndex >= 0)
        assertTrue("getAdapterOrNull(newWallet) was never called, calls: $callOrder", createIndex >= 0)
        assertTrue(
            "oldAdapter.stop() must be called BEFORE getAdapterOrNull(newWallet), " +
                "but call order was: $callOrder",
            stopIndex < createIndex
        )
    }

    /**
     * When the wallet set changes but shares common wallets with the previous set,
     * the shared wallets' adapters must be reused (not stopped and recreated).
     */
    @Test
    fun initAdapters_reusesSharedAdaptersWithoutStopping() {
        val sharedWallet: Wallet = mockk(relaxed = true)
        val removedWallet: Wallet = mockk(relaxed = true)
        val addedWallet: Wallet = mockk(relaxed = true)
        val sharedAdapter: IAdapter = mockk(relaxed = true)
        val removedAdapter: IAdapter = mockk(relaxed = true)
        val addedAdapter: IAdapter = mockk(relaxed = true)

        val initialReady = CountDownLatch(2)
        val addedReady = CountDownLatch(1)

        coEvery { adapterFactory.getAdapterOrNull(sharedWallet, any()) } coAnswers {
            sharedAdapter.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(removedWallet, any()) } coAnswers {
            removedAdapter.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(addedWallet, any()) } coAnswers {
            addedAdapter.also { addedReady.countDown() }
        }

        // Phase 1: [shared, removed]
        activeWalletsFlow.value = listOf(sharedWallet, removedWallet)
        adapterManager.startAdapterManager()
        assertTrue("Initial adapters were never created", initialReady.await(5, TimeUnit.SECONDS))

        // Phase 2: [shared, added] — shared must be reused, removed must be stopped
        activeWalletsFlow.value = listOf(sharedWallet, addedWallet)
        assertTrue("Added adapter was never created", addedReady.await(5, TimeUnit.SECONDS))

        verify(exactly = 0) { sharedAdapter.stop() }
        verify(exactly = 1) { removedAdapter.stop() }
    }

    @Test
    fun initAdapters_stopsOnlyNonReusableAdapters() {
        val walletA: Wallet = mockk(relaxed = true)
        val walletB: Wallet = mockk(relaxed = true)
        val walletC: Wallet = mockk(relaxed = true)
        val adapterA: IAdapter = mockk(relaxed = true)
        val adapterB: IAdapter = mockk(relaxed = true)
        val adapterC: IAdapter = mockk(relaxed = true)

        val callOrder = Collections.synchronizedList(mutableListOf<String>())
        val initialReady = CountDownLatch(2)
        val walletCReady = CountDownLatch(1)

        coEvery { adapterFactory.getAdapterOrNull(walletA, any()) } coAnswers {
            adapterA.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(walletB, any()) } coAnswers {
            adapterB.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(walletC, any()) } coAnswers {
            callOrder.add("getAdapterOrNull(walletC)")
            adapterC.also { walletCReady.countDown() }
        }
        every { adapterB.stop() } answers {
            callOrder.add("adapterB.stop()")
        }

        activeWalletsFlow.value = listOf(walletA, walletB)
        adapterManager.startAdapterManager()
        assertTrue("Initial adapters were never created", initialReady.await(5, TimeUnit.SECONDS))

        activeWalletsFlow.value = listOf(walletA, walletC)
        assertTrue("Adapter C was never created", walletCReady.await(5, TimeUnit.SECONDS))

        verify(exactly = 1) { adapterB.stop() }
        verify(exactly = 0) { adapterA.stop() }

        val stopIndex = callOrder.indexOf("adapterB.stop()")
        val createIndex = callOrder.indexOf("getAdapterOrNull(walletC)")
        assertTrue("adapterB.stop() was never called, calls: $callOrder", stopIndex >= 0)
        assertTrue("getAdapterOrNull(walletC) was never called, calls: $callOrder", createIndex >= 0)
        assertTrue(
            "adapterB.stop() must be called BEFORE getAdapterOrNull(walletC), " +
                "but call order was: $callOrder",
            stopIndex < createIndex
        )
    }

    @Test
    fun initAdapters_litecoinMwebEnabled_recreatesReusableLitecoinPublicAdapter() {
        val publicWallet = wallet(
            accountId = "account",
            blockchainType = BlockchainType.Litecoin,
            tokenType = TokenType.Derived(TokenType.Derivation.Bip84)
        )
        val mwebWallet = wallet(
            accountId = "account",
            blockchainType = BlockchainType.Litecoin,
            tokenType = TokenType.Mweb
        )
        val oldPublicAdapter: IAdapter = mockk(relaxed = true)
        val newPublicAdapter: IAdapter = mockk(relaxed = true)
        val mwebAdapter: IAdapter = mockk(relaxed = true)
        val oldPublicReady = CountDownLatch(1)
        val newPublicReady = CountDownLatch(1)
        val mwebReady = CountDownLatch(1)
        var publicCreates = 0

        coEvery { adapterFactory.getAdapterOrNull(publicWallet, any()) } coAnswers {
            publicCreates += 1
            if (publicCreates == 1) {
                oldPublicAdapter.also { oldPublicReady.countDown() }
            } else {
                newPublicAdapter.also { newPublicReady.countDown() }
            }
        }
        coEvery { adapterFactory.getAdapterOrNull(mwebWallet, any()) } coAnswers {
            mwebAdapter.also { mwebReady.countDown() }
        }

        activeWalletsFlow.value = listOf(publicWallet)
        adapterManager.startAdapterManager()
        assertTrue("Public adapter was never created", oldPublicReady.await(5, TimeUnit.SECONDS))

        activeWalletsFlow.value = listOf(publicWallet, mwebWallet)
        assertTrue("Public adapter was not recreated", newPublicReady.await(5, TimeUnit.SECONDS))
        assertTrue("MWEB adapter was never created", mwebReady.await(5, TimeUnit.SECONDS))

        verify(exactly = 1) { oldPublicAdapter.stop() }
        assertSame(newPublicAdapter, adapterManager.getAdapterForWallet<IAdapter>(publicWallet))
    }

    @Test
    fun initAdapters_litecoinMwebStateUnchanged_reusesLitecoinPublicAdapter() {
        val publicWallet = wallet(
            accountId = "account",
            blockchainType = BlockchainType.Litecoin,
            tokenType = TokenType.Derived(TokenType.Derivation.Bip84)
        )
        val bitcoinWallet = wallet(
            accountId = "account",
            blockchainType = BlockchainType.Bitcoin,
            tokenType = TokenType.Derived(TokenType.Derivation.Bip84)
        )
        val litecoinAdapter: IAdapter = mockk(relaxed = true)
        val bitcoinAdapter: IAdapter = mockk(relaxed = true)
        val initialReady = CountDownLatch(1)
        val bitcoinReady = CountDownLatch(1)

        coEvery { adapterFactory.getAdapterOrNull(publicWallet, any()) } coAnswers {
            litecoinAdapter.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(bitcoinWallet, any()) } coAnswers {
            bitcoinAdapter.also { bitcoinReady.countDown() }
        }

        activeWalletsFlow.value = listOf(publicWallet)
        adapterManager.startAdapterManager()
        assertTrue("Litecoin adapter was never created", initialReady.await(5, TimeUnit.SECONDS))

        activeWalletsFlow.value = listOf(publicWallet, bitcoinWallet)
        assertTrue("Bitcoin adapter was never created", bitcoinReady.await(5, TimeUnit.SECONDS))

        coVerify(exactly = 1) { adapterFactory.getAdapterOrNull(publicWallet, any()) }
        verify(exactly = 0) { litecoinAdapter.stop() }
        assertSame(litecoinAdapter, adapterManager.getAdapterForWallet<IAdapter>(publicWallet))
    }

    @Test
    fun stopAdapters_accountId_stopsOnlyMatchingAdapters() = runTest {
        val targetWallet = wallet("target")
        val otherWallet = wallet("other")
        val targetAdapter: IAdapter = mockk(relaxed = true)
        val otherAdapter: IAdapter = mockk(relaxed = true)
        val ready = CountDownLatch(2)

        coEvery { adapterFactory.getAdapterOrNull(targetWallet, any()) } coAnswers {
            targetAdapter.also { ready.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(otherWallet, any()) } coAnswers {
            otherAdapter.also { ready.countDown() }
        }

        activeWalletsFlow.value = listOf(targetWallet, otherWallet)
        adapterManager.startAdapterManager()
        assertTrue("Adapters were never created", ready.await(5, TimeUnit.SECONDS))

        adapterManager.stopAdapters(listOf("target"))

        verify(exactly = 1) { targetAdapter.stop() }
        verify(exactly = 0) { otherAdapter.stop() }
        coVerify(exactly = 1) { adapterFactory.unlinkAdapter(targetWallet) }
        assertNull(adapterManager.getAdapterForWallet<IAdapter>(targetWallet))
        assertSame(otherAdapter, adapterManager.getAdapterForWallet<IAdapter>(otherWallet))
    }

    @Test
    fun stopAdapters_accountIdAndBlockchain_stopsOnlyMatchingBlockchainAdapters() = runTest {
        val targetLitecoinWallet = wallet("target", BlockchainType.Litecoin, TokenType.Native)
        val targetBitcoinWallet = wallet("target", BlockchainType.Bitcoin, TokenType.Native)
        val otherLitecoinWallet = wallet("other", BlockchainType.Litecoin, TokenType.Native)
        val targetLitecoinAdapter: IAdapter = mockk(relaxed = true)
        val targetBitcoinAdapter: IAdapter = mockk(relaxed = true)
        val otherLitecoinAdapter: IAdapter = mockk(relaxed = true)
        val ready = CountDownLatch(3)

        coEvery { adapterFactory.getAdapterOrNull(targetLitecoinWallet, any()) } coAnswers {
            targetLitecoinAdapter.also { ready.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(targetBitcoinWallet, any()) } coAnswers {
            targetBitcoinAdapter.also { ready.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(otherLitecoinWallet, any()) } coAnswers {
            otherLitecoinAdapter.also { ready.countDown() }
        }

        activeWalletsFlow.value = listOf(targetLitecoinWallet, targetBitcoinWallet, otherLitecoinWallet)
        adapterManager.startAdapterManager()
        assertTrue("Adapters were never created", ready.await(5, TimeUnit.SECONDS))

        adapterManager.stopAdapters(listOf("target"), BlockchainType.Litecoin)

        verify(exactly = 1) { targetLitecoinAdapter.stop() }
        verify(exactly = 0) { targetBitcoinAdapter.stop() }
        verify(exactly = 0) { otherLitecoinAdapter.stop() }
        coVerify(exactly = 1) { adapterFactory.unlinkAdapter(targetLitecoinWallet) }
        coVerify(exactly = 0) { adapterFactory.unlinkAdapter(targetBitcoinWallet) }
        coVerify(exactly = 0) { adapterFactory.unlinkAdapter(otherLitecoinWallet) }
        assertNull(adapterManager.getAdapterForWallet<IAdapter>(targetLitecoinWallet))
        assertSame(targetBitcoinAdapter, adapterManager.getAdapterForWallet<IAdapter>(targetBitcoinWallet))
        assertSame(otherLitecoinAdapter, adapterManager.getAdapterForWallet<IAdapter>(otherLitecoinWallet))
    }

    private fun wallet(accountId: String): Wallet {
        return wallet(accountId, BlockchainType.Bitcoin, TokenType.Native)
    }

    private fun wallet(
        accountId: String,
        blockchainType: BlockchainType,
        tokenType: TokenType,
    ): Wallet {
        val account = mockk<Account> {
            every { id } returns accountId
        }
        val token = mockk<Token> {
            every { this@mockk.blockchainType } returns blockchainType
            every { type } returns tokenType
        }
        return mockk {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }
    }
}
