package cash.p.terminal.modules.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.p.terminal.wallet.managers.TransactionHiddenState
import cash.p.terminal.wallet.transaction.TransactionSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

/**
 * Tests that wallet switch does not show stale transactions.
 *
 * Verifies that when user switches between wallets (accounts), the ViewModel:
 * 1. Immediately shows Loading state (not old transactions)
 * 2. Shows new transactions arriving after switch (defense is at service/repo layer)
 * 3. Shows correct transactions once new data arrives after adapters ready
 * 4. Handles edge case of switching between empty-transaction accounts
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelWalletSwitchTest : KoinTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    // Mocks
    private val service = mockk<TransactionsService>(relaxed = true)
    private val transactionViewItemFactory = mockk<TransactionViewItemFactory>(relaxed = true)
    private val balanceHiddenManager = mockk<BalanceHiddenManager>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val walletManager = mockk<IWalletManager>(relaxed = true)
    private val transactionFilterService = mockk<TransactionFilterService>(relaxed = true)
    private val transactionHiddenManager = mockk<TransactionHiddenManager>(relaxed = true)
    private val premiumSettings = mockk<PremiumSettings>(relaxed = true)
    private val amlStatusManager = mockk<AmlStatusManager>(relaxed = true)
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val swapProviderTransactionsStorage = mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val contactsRepository = mockk<ContactsRepository>(relaxed = true)

    // Controllable flows
    private lateinit var activeWalletsFlow: MutableStateFlow<List<Wallet>>
    private lateinit var adaptersReadyFlow: MutableStateFlow<Map<TransactionSource, ITransactionsAdapter>>
    private lateinit var initializationFlow: MutableStateFlow<Boolean>
    private lateinit var transactionItemsFlow: MutableStateFlow<List<TransactionItem>>
    private lateinit var filterStateFlow: MutableStateFlow<TransactionFilterService.State>
    private lateinit var syncingFlow: MutableStateFlow<Boolean>
    private lateinit var transactionHiddenFlow: MutableStateFlow<TransactionHiddenState>

    private var adapterVersion = 0

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { mockk<cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase>(relaxed = true) }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        activeWalletsFlow = MutableStateFlow(emptyList())
        adaptersReadyFlow = MutableStateFlow(emptyMap())
        initializationFlow = MutableStateFlow(false)
        transactionItemsFlow = MutableStateFlow(emptyList())
        syncingFlow = MutableStateFlow(false)
        transactionHiddenFlow = MutableStateFlow(
            TransactionHiddenState(
                transactionHidden = false,
                transactionHideEnabled = false,
                transactionDisplayLevel = TransactionDisplayLevel.LAST_2_TRANSACTIONS,
                transactionAutoHidePinExists = false
            )
        )

        filterStateFlow = MutableStateFlow(
            TransactionFilterService.State(
                blockchains = listOf(null),
                selectedBlockchain = null,
                filterTokens = listOf(null),
                selectedToken = null,
                transactionTypes = FilterTransactionType.values().toList(),
                selectedTransactionType = FilterTransactionType.All,
                resetEnabled = false,
                uniqueId = "test",
                contact = null,
                hideSuspiciousTx = false,
            )
        )

        every { walletManager.activeWalletsFlow } returns activeWalletsFlow
        every { walletManager.activeWallets } returns emptyList()
        every { transactionAdapterManager.adaptersReadyFlow } returns adaptersReadyFlow
        every { transactionAdapterManager.initializationFlow } returns initializationFlow
        every { service.transactionItemsFlow } returns transactionItemsFlow
        every { service.syncingFlow } returns syncingFlow
        every { transactionFilterService.stateFlow } returns filterStateFlow
        every { transactionHiddenManager.transactionHiddenFlow } returns transactionHiddenFlow
        every { balanceHiddenManager.balanceHiddenFlow } returns MutableStateFlow(false)
        every { balanceHiddenManager.balanceHidden } returns false
        every { balanceHiddenManager.anyTransactionVisibilityChangedFlow } returns MutableSharedFlow()
        every { contactsRepository.contactsFlow } returns MutableStateFlow(emptyList())
        every { amlStatusManager.statusUpdates } returns MutableSharedFlow()
        every { amlStatusManager.enabledStateFlow } returns MutableStateFlow(false)
        every { amlStatusManager.isEnabled } returns false
        every { amlStatusManager.applyStatus(any()) } answers { firstArg() }
        every { premiumSettings.getAmlCheckShowAlert() } returns false
        every { swapProviderTransactionsStorage.observeAll() } returns flowOf(emptyList())

        every { transactionViewItemFactory.convertToViewItemCached(any(), any(), any()) } answers {
            createMockViewItem(firstArg<TransactionItem>().record.uid)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Wallet Switch Tests

    @Test
    fun walletSwitch_showsLoadingAndHidesOldTransactions() = runTest(dispatcher) {
        val walletsA = listOf(createWallet("account-A"))
        every { walletManager.activeWallets } returns walletsA
        activeWalletsFlow.value = walletsA

        val viewModel = createViewModel()
        advanceUntilIdle()

        emitAdaptersReady()
        advanceUntilIdle()
        transactionItemsFlow.value = listOf(
            createTransactionItem("tx-a1"),
            createTransactionItem("tx-a2"),
            createTransactionItem("tx-a3"),
        )
        advanceUntilIdle()

        assertEquals(ViewState.Success, viewModel.uiState.viewState)
        assertEquals(3, viewModel.uiState.transactions?.values?.flatten()?.size)

        // When: switch to wallet B
        val walletsB = listOf(createWallet("account-B"))
        every { walletManager.activeWallets } returns walletsB
        activeWalletsFlow.value = walletsB
        advanceUntilIdle()

        // Then: must show Loading, not old transactions
        assertEquals(ViewState.Loading, viewModel.uiState.viewState)
        assertNull(viewModel.uiState.transactions)
    }

    @Test
    fun walletSwitch_showsItemsArrivingAfterSwitch() = runTest(dispatcher) {
        val walletsA = listOf(createWallet("account-A"))
        every { walletManager.activeWallets } returns walletsA
        activeWalletsFlow.value = walletsA

        val viewModel = createViewModel()
        advanceUntilIdle()

        emitAdaptersReady()
        advanceUntilIdle()
        transactionItemsFlow.value = listOf(
            createTransactionItem("tx-a1"),
            createTransactionItem("tx-a2"),
            createTransactionItem("tx-a3"),
        )
        advanceUntilIdle()

        // When: switch to wallet B
        val walletsB = listOf(createWallet("account-B"))
        every { walletManager.activeWallets } returns walletsB
        activeWalletsFlow.value = walletsB
        advanceUntilIdle()

        // Items arrive from new account (service/repo layer prevents stale data)
        transactionItemsFlow.value = listOf(createTransactionItem("tx-b1"))
        advanceUntilIdle()

        // Then: items are processed normally — defense is at service/repo layer
        assertEquals(ViewState.Success, viewModel.uiState.viewState)
        val shownItems = viewModel.uiState.transactions?.values?.flatten()
        assertEquals(1, shownItems?.size)
        assertEquals("tx-b1", shownItems?.first()?.uid)
    }

    @Test
    fun walletSwitch_showsNewTransactionsAfterAdaptersReady() = runTest(dispatcher) {
        val walletsA = listOf(createWallet("account-A"))
        every { walletManager.activeWallets } returns walletsA
        activeWalletsFlow.value = walletsA

        val viewModel = createViewModel()
        advanceUntilIdle()

        emitAdaptersReady()
        advanceUntilIdle()
        transactionItemsFlow.value = listOf(
            createTransactionItem("tx-a1"),
            createTransactionItem("tx-a2"),
            createTransactionItem("tx-a3"),
        )
        advanceUntilIdle()

        // When: switch to wallet B
        val walletsB = listOf(createWallet("account-B"))
        every { walletManager.activeWallets } returns walletsB
        activeWalletsFlow.value = walletsB
        advanceUntilIdle()

        // Adapters become ready, then new items arrive
        emitAdaptersReady()
        advanceUntilIdle()
        transactionItemsFlow.value = listOf(createTransactionItem("tx-b1"))
        advanceUntilIdle()

        // Then: must show wallet B transactions
        assertEquals(ViewState.Success, viewModel.uiState.viewState)
        val shownItems = viewModel.uiState.transactions?.values?.flatten()
        assertEquals(1, shownItems?.size)
        assertEquals("tx-b1", shownItems?.first()?.uid)
    }

    @Test
    fun walletSwitch_emptyToEmpty_doesNotStayOnLoadingForever() = runTest(dispatcher) {
        val walletsA = listOf(createWallet("account-A"))
        every { walletManager.activeWallets } returns walletsA
        activeWalletsFlow.value = walletsA

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Adapters ready, items stay empty
        emitAdaptersReady()
        advanceUntilIdle()

        // When: switch to wallet B which also has 0 transactions
        val walletsB = listOf(createWallet("account-B"))
        every { walletManager.activeWallets } returns walletsB
        activeWalletsFlow.value = walletsB
        advanceUntilIdle()

        // Adapters become ready for wallet B
        // transactionItemsFlow is still emptyList() — StateFlow dedup prevents emission
        emitAdaptersReady()
        advanceUntilIdle()

        // All adapters initialized — clears awaitingAdaptersAfterSwitch for empty accounts
        initializationFlow.value = true
        advanceUntilIdle()

        // Then: must NOT be stuck on Loading — should show Success with empty list
        assertEquals(ViewState.Success, viewModel.uiState.viewState)
        assertTrue(viewModel.uiState.transactions?.isEmpty() == true)
    }

    // endregion

    // region Helpers

    private fun CoroutineScope.createViewModel() = TransactionsViewModel(
        service = service,
        transactionViewItem2Factory = transactionViewItemFactory,
        balanceHiddenManager = balanceHiddenManager,
        transactionAdapterManager = transactionAdapterManager,
        walletManager = walletManager,
        transactionFilterService = transactionFilterService,
        transactionHiddenManager = transactionHiddenManager,
        premiumSettings = premiumSettings,
        amlStatusManager = amlStatusManager,
        adapterManager = adapterManager,
        swapProviderTransactionsStorage = swapProviderTransactionsStorage,
        contactsRepository = contactsRepository,
        dispatcherProvider = TestDispatcherProvider(dispatcher, this),
    )

    private fun emitAdaptersReady() {
        adapterVersion++
        val source = mockk<TransactionSource>(relaxed = true)
        val adapter = mockk<ITransactionsAdapter>(relaxed = true)
        adaptersReadyFlow.value = HashMap<TransactionSource, ITransactionsAdapter>().apply {
            put(source, adapter)
        }
    }

    private fun createWallet(accountId: String): Wallet {
        val account = Account(
            id = accountId,
            name = "Test $accountId",
            type = mockk(relaxed = true),
            origin = AccountOrigin.Created,
            level = 0,
        )
        val wallet = mockk<Wallet>(relaxed = true)
        every { wallet.account } returns account
        every { wallet.transactionSource } returns mockk(relaxed = true)
        return wallet
    }

    private fun createTransactionItem(uid: String): TransactionItem {
        val record = mockk<TransactionRecord>(relaxed = true) {
            every { this@mockk.uid } returns uid
            every { timestamp } returns System.currentTimeMillis() / 1000
            every { spam } returns false
        }
        return TransactionItem(
            record = record,
            currencyValue = null,
            lastBlockInfo = null,
            nftMetadata = emptyMap(),
        )
    }

    private fun createMockViewItem(uid: String) = mockk<TransactionViewItem>(relaxed = true) {
        every { this@mockk.uid } returns uid
        every { formattedDate } returns "DATE"
    }

    // endregion
}
