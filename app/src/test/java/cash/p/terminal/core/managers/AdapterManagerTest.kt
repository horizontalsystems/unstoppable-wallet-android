package cash.p.terminal.core.managers

import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
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

        coEvery { adapterFactory.getAdapterOrNull(oldWallet) } coAnswers {
            oldAdapter.also { oldAdapterReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(newWallet) } coAnswers {
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

        coEvery { adapterFactory.getAdapterOrNull(sharedWallet) } coAnswers {
            sharedAdapter.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(removedWallet) } coAnswers {
            removedAdapter.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(addedWallet) } coAnswers {
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

        coEvery { adapterFactory.getAdapterOrNull(walletA) } coAnswers {
            adapterA.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(walletB) } coAnswers {
            adapterB.also { initialReady.countDown() }
        }
        coEvery { adapterFactory.getAdapterOrNull(walletC) } coAnswers {
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
}
