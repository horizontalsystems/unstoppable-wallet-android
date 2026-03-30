package cash.p.terminal.modules.balance.token

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewMode
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.displayoptions.DisplayPricePeriod
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.SyncingProgress
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService
import cash.p.terminal.modules.transactions.TransactionItem
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionViewItemFactory
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.p.terminal.wallet.managers.TransactionHiddenState
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.tokenQueryId
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.math.BigDecimal
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
 * Unit tests for TokenBalanceViewModel focusing on the auto-hide transactions feature.
 *
 * These tests verify that when transactionHiddenFlow emits:
 * 1. The flow is collected (transactionsService.refreshList() is called)
 * 2. Cached transactions are re-processed (refreshTransactionsFromCache effect)
 * 3. The ViewModel properly delegates to TransactionHiddenManager
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenBalanceViewModelTest : KoinTest {

    private val dispatcher = UnconfinedTestDispatcher()

    // Mocks
    private val totalBalance = mockk<TotalBalance>(relaxed = true)
    private val balanceService = mockk<TokenBalanceService>(relaxed = true)
    private val balanceViewItemFactory = mockk<BalanceViewItemFactory>(relaxed = true)
    private val transactionsService = mockk<TokenTransactionsService>(relaxed = true)
    private val transactionViewItemFactory = mockk<TransactionViewItemFactory>(relaxed = true)
    private val balanceHiddenManager = mockk<IBalanceHiddenManager>()
    private val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    private val accountManager = mockk<IAccountManager>(relaxed = true)
    private val transactionHiddenManager = mockk<TransactionHiddenManager>()
    private val getChangeNowAssociatedCoinTickerUseCase = mockk<GetChangeNowAssociatedCoinTickerUseCase>()
    private val premiumSettings = mockk<PremiumSettings>()
    private val amlStatusManager = mockk<AmlStatusManager>()
    private val marketFavoritesManager = mockk<MarketFavoritesManager>(relaxed = true)
    private val stackingManager = mockk<StackingManager>(relaxed = true)
    private val priceManager = mockk<PriceManager>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val numberFormatter = mockk<io.horizontalsystems.core.IAppNumberFormatter>(relaxed = true)
    private val contactsRepository = mockk<ContactsRepository>(relaxed = true)

    // Controllable flows
    private lateinit var transactionHiddenFlow: MutableStateFlow<TransactionHiddenState>
    private lateinit var transactionItemsFlow: MutableStateFlow<List<TransactionItem>>
    private lateinit var balanceItemFlow: MutableStateFlow<BalanceItem?>
    private lateinit var walletBalanceHiddenFlow: MutableStateFlow<Boolean>
    private lateinit var anyTransactionVisibilityChangedFlow: MutableSharedFlow<Unit>
    private lateinit var amlStatusUpdates: MutableSharedFlow<AmlStatusManager.AmlStatusUpdate>
    private lateinit var amlEnabledStateFlow: MutableStateFlow<Boolean>
    private lateinit var syncingFlow: MutableStateFlow<Boolean>
    private lateinit var recordsLoadedFlow: MutableStateFlow<Boolean>

    private lateinit var testWallet: Wallet

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { mockk<UpdateSwapProviderTransactionsStatusUseCase>(relaxed = true) }
                single {
                    mockk<IAdapterManager>(relaxed = true) {
                        coEvery { awaitAdapterForWallet<IReceiveAdapter>(any(), any()) } returns null
                    }
                }
                single {
                    mockk<SwapProviderTransactionsStorage>(relaxed = true) {
                        every { observeByToken(any(), any(), any()) } returns flowOf(emptyList())
                    }
                }
                single { mockk<cash.p.terminal.wallet.MarketKitWrapper>(relaxed = true) }
                single {
                    mockk<PoisonAddressManager>(relaxed = true) {
                        every { poisonDbChangedFlow } returns MutableSharedFlow()
                    }
                }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        CoreApp.instance = mockk(relaxed = true) {
            every { isSwapEnabled } returns false
            every { getString(any(), *anyVararg()) } returns ""
        }

        transactionHiddenFlow = MutableStateFlow(createHiddenState(hidden = false))
        transactionItemsFlow = MutableStateFlow(emptyList())
        balanceItemFlow = MutableStateFlow(null)
        walletBalanceHiddenFlow = MutableStateFlow(false)
        anyTransactionVisibilityChangedFlow = MutableSharedFlow()
        amlStatusUpdates = MutableSharedFlow()
        amlEnabledStateFlow = MutableStateFlow(false)

        testWallet = createTestWallet()

        every { transactionHiddenManager.transactionHiddenFlow } returns transactionHiddenFlow
        every { transactionHiddenManager.showAllTransactions(any()) } returns Unit
        every { transactionsService.transactionItemsFlow } returns transactionItemsFlow
        syncingFlow = MutableStateFlow(true)
        recordsLoadedFlow = MutableStateFlow(false)
        every { transactionsService.syncingFlow } returns syncingFlow
        every { transactionsService.recordsLoadedFlow } returns recordsLoadedFlow
        every { transactionsService.refreshList() } returns Unit
        every { balanceService.balanceItemFlow } returns balanceItemFlow
        every { balanceService.balanceItem } returns null
        every { balanceHiddenManager.walletBalanceHiddenFlow(any()) } returns walletBalanceHiddenFlow
        every { balanceHiddenManager.isWalletBalanceHidden(any()) } returns false
        every { balanceHiddenManager.anyTransactionVisibilityChangedFlow } returns anyTransactionVisibilityChangedFlow
        every { contactsRepository.contactsFlow } returns MutableStateFlow(emptyList())
        every { amlStatusManager.statusUpdates } returns amlStatusUpdates
        every { amlStatusManager.enabledStateFlow } returns amlEnabledStateFlow
        every { amlStatusManager.isEnabled } returns false
        every { amlStatusManager.applyStatus(any()) } answers { firstArg() }
        every { premiumSettings.getAmlCheckShowAlert() } returns false
        every { totalBalance.stateFlow } returns MutableStateFlow(TotalService.State.Hidden)
        every { stackingManager.unpaidFlow } returns MutableStateFlow<BigDecimal?>(BigDecimal.ZERO)
        every { priceManager.displayPricePeriodFlow } returns MutableStateFlow(DisplayPricePeriod.ONE_DAY)
        every { priceManager.displayDiffOptionTypeFlow } returns MutableStateFlow(DisplayDiffOptionType.BOTH)
        every { localStorage.displayDiffPricePeriod } returns DisplayPricePeriod.ONE_DAY
        every { localStorage.displayDiffOptionType } returns DisplayDiffOptionType.BOTH
        every { localStorage.isRoundingAmountMainPage } returns false
        coEvery { getChangeNowAssociatedCoinTickerUseCase(any(), any()) } returns null
        every { transactionViewItemFactory.convertToViewItemCached(any(), any(), any()) } answers {
            createMockTransactionViewItem(firstArg<TransactionItem>().record.uid)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Core Fix Tests (MOBILE-469)

    @Test
    fun transactionHiddenFlowEmits_callsRefreshList() = runTest(dispatcher) {
        // Given: ViewModel initialized
        createViewModel()
        advanceUntilIdle()

        // Clear call counts from initialization
        clearMocks(transactionsService, answers = false)

        // When: transactionHiddenFlow emits new value
        transactionHiddenFlow.value = createHiddenState(hidden = true)
        advanceUntilIdle()

        // Then: refreshList() must be called (this is the core fix)
        verify(exactly = 1) { transactionsService.refreshList() }
    }

    @Test
    fun transactionHiddenFlowEmitsMultipleTimes_callsRefreshListEachTime() = runTest(dispatcher) {
        // Given: ViewModel initialized
        createViewModel()
        advanceUntilIdle()
        clearMocks(transactionsService, answers = false)

        // When: transactionHiddenFlow emits twice
        transactionHiddenFlow.value = createHiddenState(hidden = true)
        advanceUntilIdle()
        transactionHiddenFlow.value = createHiddenState(hidden = false)
        advanceUntilIdle()

        // Then: refreshList() must be called at least twice
        verify(exactly = 2) { transactionsService.refreshList() }
    }

    @Test
    fun transactionHiddenFlowEmits_updatesTransactionsFromCache() = runTest(dispatcher) {
        // Given: cached transactions are loaded before hidden state changes
        transactionItemsFlow.value = listOf(
            createTransactionItem("tx-1"),
            createTransactionItem("tx-2")
        )
        transactionHiddenFlow.value = createHiddenState(hidden = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.transactions?.values?.flatten()?.size)
        assertEquals(false, viewModel.uiState.hasHiddenTransactions)

        // When: transactionHiddenFlow emits (no new transaction items)
        transactionHiddenFlow.value = createHiddenState(
            hidden = true,
            level = TransactionDisplayLevel.LAST_1_TRANSACTION
        )
        advanceUntilIdle()

        // Then: cached transactions are re-processed using the new hidden state
        assertEquals(1, viewModel.uiState.transactions?.values?.flatten()?.size)
        assertEquals(true, viewModel.uiState.hasHiddenTransactions)
    }

    // endregion

    // region Sync Message Tests (MOBILE-583)

    @Test
    fun updateTransactions_emptyItemsDuringSyncing_transactionsStaysNull() = runTest(dispatcher) {
        syncingFlow.value = true
        recordsLoadedFlow.value = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Empty items arrive while syncing — guard should block
        transactionItemsFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.transactions)
        assertEquals(true, viewModel.uiState.syncing)
    }

    @Test
    fun updateTransactions_syncFinishesNoTransactions_showsEmptyNotNull() = runTest(dispatcher) {
        syncingFlow.value = true
        recordsLoadedFlow.value = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Records loaded, empty items emitted — guard blocks (still syncing)
        recordsLoadedFlow.value = true
        transactionItemsFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.transactions)

        // Sync finishes — re-trigger should set transactions to empty map
        syncingFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.syncing)
        assertEquals(emptyMap<String, List<TransactionViewItem>>(), viewModel.uiState.transactions)
    }

    @Test
    fun updateTransactions_syncFinishesWithTransactions_showsTransactions() = runTest(dispatcher) {
        syncingFlow.value = true
        recordsLoadedFlow.value = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Non-empty items arrive — guard does not block
        recordsLoadedFlow.value = true
        transactionItemsFlow.value = listOf(createTransactionItem("tx-1"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.transactions?.values?.flatten()?.size)

        // Sync finishes — transactions remain
        syncingFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.syncing)
        assertEquals(1, viewModel.uiState.transactions?.values?.flatten()?.size)
    }

    // endregion

    // region Delegation Tests

    @Test
    fun showAllTransactions_delegatesToManager() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showAllTransactions(true)
        verify(exactly = 1) { transactionHiddenManager.showAllTransactions(true) }

        viewModel.showAllTransactions(false)
        verify(exactly = 1) { transactionHiddenManager.showAllTransactions(false) }
    }

    // endregion

    // region Secondary Value Tests (MOBILE-517)

    @Test
    fun secondaryValue_globalBalanceHiddenPerWalletRevealed_showsFiatValue() = runTest(dispatcher) {
        val expectedFiat = "$142.35"

        // Global balance hidden → TotalService emits State.Hidden
        val totalStateFlow = MutableStateFlow<TotalService.State>(TotalService.State.Hidden)
        every { totalBalance.stateFlow } returns totalStateFlow

        // balanceViewItemFactory returns a view item with real fiat value (visible)
        val balanceViewItem = createBalanceViewItem(
            secondaryValue = DeemedValue(value = expectedFiat, dimmed = false, visible = true)
        )
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns balanceViewItem
        every { balanceHiddenManager.isWalletBalanceHidden(any()) } returns false

        val testBalanceItem = createBalanceItem()
        every { balanceService.balanceItem } returns testBalanceItem

        // Create ViewModel, then emit balance data
        val viewModel = createViewModel()
        balanceItemFlow.value = testBalanceItem
        advanceUntilIdle()

        // Simulate per-wallet reveal (tap on token screen)
        walletBalanceHiddenFlow.value = false
        advanceUntilIdle()

        // Secondary value must show fiat, not be empty
        assertEquals(expectedFiat, viewModel.secondaryValue.value)
        assertEquals(true, viewModel.secondaryValue.visible)
    }

    // endregion

    // region Staking Status Tests (MOBILE-588)

    @Test
    fun stakingStatus_balanceAboveThreshold_showsActive() = runTest(dispatcher) {
        val pirateWallet = createPirateCashWallet()
        testWallet = pirateWallet

        val balanceItem = createBalanceItem(balance = BigDecimal("150"), wallet = pirateWallet)
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        advanceUntilIdle()

        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(TokenBalanceModule.StakingStatus.ACTIVE, viewModel.uiState.stakingStatus)
    }

    @Test
    fun stakingStatus_balanceBelowThreshold_showsInactive() = runTest(dispatcher) {
        val pirateWallet = createPirateCashWallet()
        testWallet = pirateWallet

        val balanceItem = createBalanceItem(balance = BigDecimal("1"), wallet = pirateWallet)
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(TokenBalanceModule.StakingStatus.INACTIVE, viewModel.uiState.stakingStatus)
    }

    @Test
    fun stakingStatus_balanceBelowThresholdWithUnpaidRewards_showsInactive() = runTest(dispatcher) {
        val pirateWallet = createPirateCashWallet()
        testWallet = pirateWallet

        // Simulate unpaid rewards from StackingManager
        val unpaidFlow = MutableStateFlow<BigDecimal?>(BigDecimal("0.7897"))
        every { stackingManager.unpaidFlow } returns unpaidFlow

        val balanceItem = createBalanceItem(balance = BigDecimal("1"), wallet = pirateWallet)
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        // MOBILE-588: Must show INACTIVE even when unpaid rewards exist
        assertEquals(TokenBalanceModule.StakingStatus.INACTIVE, viewModel.uiState.stakingStatus)
    }

    @Test
    fun stakingStatus_balanceExactlyAtThreshold_showsActive() = runTest(dispatcher) {
        val pirateWallet = createPirateCashWallet()
        testWallet = pirateWallet

        val balanceItem = createBalanceItem(balance = BigDecimal("100"), wallet = pirateWallet)
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(TokenBalanceModule.StakingStatus.ACTIVE, viewModel.uiState.stakingStatus)
    }

    // endregion

    // region Network Fee Warning Tests (MOBILE-526)

    @Test
    fun networkFeeWarning_nativeToken_noWarning() = runTest(dispatcher) {
        val balanceItem = createBalanceItem()
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.networkFeeWarning)
    }

    @Test
    fun networkFeeWarning_nonNativeTokenZeroBalance_showsWarning() = runTest(dispatcher) {
        val bep20Wallet = createBep20Wallet()
        testWallet = bep20Wallet
        setupFeeWarningMocks()

        val viewModel = createViewModel()
        val balanceItem = createBalanceItem(wallet = bep20Wallet)
        every { balanceService.balanceItem } returns balanceItem
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.networkFeeWarning != null)
    }

    @Test
    fun networkFeeWarning_sufficientNativeBalance_noWarning() = runTest(dispatcher) {
        val bep20Wallet = createBep20Wallet()
        testWallet = bep20Wallet
        setupFeeWarningMocks()
        setupNativeBalanceMocks(nativeBalance = BigDecimal("0.1"))

        val balanceItem = createBalanceItem(wallet = bep20Wallet)
        every { balanceService.balanceItem } returns balanceItem
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.networkFeeWarning)
    }

    @Test
    fun networkFeeWarning_exactlyAtThreshold_noWarning() = runTest(dispatcher) {
        val tronWallet = createTrc20Wallet()
        testWallet = tronWallet
        setupNativeBalanceMocks(
            nativeBalance = BigDecimal("50"),
            nativeCoinCode = "TRX",
            blockchainType = BlockchainType.Tron,
            blockchainName = "TRON"
        )
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        val balanceItem = createBalanceItem(wallet = tronWallet)
        every { balanceService.balanceItem } returns balanceItem
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.networkFeeWarning)
    }

    @Test
    fun networkFeeWarning_justBelowThreshold_showsWarning() = runTest(dispatcher) {
        val tronWallet = createTrc20Wallet()
        testWallet = tronWallet
        setupNativeBalanceMocks(
            nativeBalance = BigDecimal("49.99"),
            nativeCoinCode = "TRX",
            blockchainType = BlockchainType.Tron,
            blockchainName = "TRON"
        )
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()

        val viewModel = createViewModel()
        val balanceItem = createBalanceItem(wallet = tronWallet)
        every { balanceService.balanceItem } returns balanceItem
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.networkFeeWarning != null)
    }

    @Test
    fun networkFeeWarning_noNativeWalletAdded_showsWarning() = runTest(dispatcher) {
        val bep20Wallet = createBep20Wallet()
        testWallet = bep20Wallet
        setupFeeWarningMocks()

        // getBalanceAdapterForWallet returns adapter that does NOT implement INativeBalanceProvider
        val mockAdapterManager = getKoin().get<cash.p.terminal.wallet.IAdapterManager>()
        every { mockAdapterManager.getBalanceAdapterForWallet(any()) } returns mockk(relaxed = true)

        val balanceItem = createBalanceItem(wallet = bep20Wallet)
        every { balanceService.balanceItem } returns balanceItem

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.networkFeeWarning != null)
    }

    @Test
    fun networkFeeWarning_dismissed_noWarning() = runTest(dispatcher) {
        val bep20Wallet = createBep20Wallet()
        testWallet = bep20Wallet
        setupFeeWarningMocks()
        every { localStorage.isNetworkFeeWarningDismissed(any()) } returns true

        val balanceItem = createBalanceItem(wallet = bep20Wallet)
        every { balanceService.balanceItem } returns balanceItem

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.networkFeeWarning)
    }

    @Test
    fun dismissNetworkFeeWarning_persistsAndClearsWarning() = runTest(dispatcher) {
        val bep20Wallet = createBep20Wallet()
        testWallet = bep20Wallet
        setupFeeWarningMocks()

        val balanceItem = createBalanceItem(wallet = bep20Wallet)
        every { balanceService.balanceItem } returns balanceItem

        val viewModel = createViewModel()
        balanceItemFlow.value = balanceItem
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.networkFeeWarning != null)

        viewModel.dismissNetworkFeeWarning()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.networkFeeWarning)
        verify { localStorage.dismissNetworkFeeWarning(BlockchainType.BinanceSmartChain.uid) }
    }

    private fun setupFeeWarningMocks() {
        val nativeToken = mockk<Token>(relaxed = true) {
            every { coin } returns Coin(uid = "bnb", name = "BNB", code = "BNB")
            every { decimals } returns 18
        }
        val mockMarketKit = getKoin().get<cash.p.terminal.wallet.MarketKitWrapper>()
        every { mockMarketKit.token(any()) } returns nativeToken
        every { mockMarketKit.blockchain(any<String>()) } returns Blockchain(
            type = BlockchainType.BinanceSmartChain, name = "BNB Smart Chain", eip3091url = null
        )
        every { numberFormatter.formatCoinShort(any(), any(), any()) } returns "0"
        every { balanceViewItemFactory.viewItem(any(), any(), any(), any(), any(), any(), any()) } returns createBalanceViewItem()
    }

    private fun setupNativeBalanceMocks(
        nativeBalance: BigDecimal,
        nativeCoinCode: String = "BNB",
        blockchainType: BlockchainType = BlockchainType.BinanceSmartChain,
        blockchainName: String = "BNB Smart Chain"
    ) {
        val nativeToken = mockk<Token>(relaxed = true) {
            every { coin } returns Coin(uid = nativeCoinCode.lowercase(), name = nativeCoinCode, code = nativeCoinCode)
            every { decimals } returns 18
        }
        val mockMarketKit = getKoin().get<cash.p.terminal.wallet.MarketKitWrapper>()
        every { mockMarketKit.token(any()) } returns nativeToken
        every { mockMarketKit.blockchain(any<String>()) } returns Blockchain(
            type = blockchainType, name = blockchainName, eip3091url = null
        )

        val mockAdapterManager = getKoin().get<cash.p.terminal.wallet.IAdapterManager>()
        val balanceAdapter = object : cash.p.terminal.wallet.IBalanceAdapter, cash.p.terminal.core.INativeBalanceProvider {
            override val nativeBalanceData = BalanceData(available = nativeBalance)
            override val nativeBalanceUpdatedFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
            override val balanceData get() = BalanceData(available = BigDecimal.ZERO)
            override val balanceStateUpdatedFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
            override val balanceState get() = AdapterState.Synced
            override val balanceUpdatedFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
        }
        every { mockAdapterManager.getBalanceAdapterForWallet(any()) } returns balanceAdapter

        every { numberFormatter.formatCoinShort(any(), any(), any()) } returns nativeBalance.toPlainString()
        every { CoreApp.instance.getString(any(), *anyVararg()) } answers { "warning text" }
    // region Address Poisoning View Mode Tests

    @Test
    fun refreshTransactionDisplaySettings_modeNotChanged_doesNotRefresh() = runTest(dispatcher) {
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD

        val viewModel = createViewModel()
        advanceUntilIdle()
        clearMocks(transactionViewItemFactory, answers = false)

        viewModel.refreshTransactionDisplaySettings()

        verify(exactly = 0) { transactionViewItemFactory.updateCache() }
    }

    @Test
    fun refreshTransactionDisplaySettings_modeChanged_refreshesCache() = runTest(dispatcher) {
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD

        val viewModel = createViewModel()
        advanceUntilIdle()
        clearMocks(transactionViewItemFactory, answers = false)

        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.COMPACT

        viewModel.refreshTransactionDisplaySettings()

        verify(exactly = 1) { transactionViewItemFactory.updateCache() }
    }

    @Test
    fun refreshTransactionDisplaySettings_modeChangedBack_refreshesTwice() = runTest(dispatcher) {
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD

        val viewModel = createViewModel()
        advanceUntilIdle()
        clearMocks(transactionViewItemFactory, answers = false)

        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.COMPACT
        viewModel.refreshTransactionDisplaySettings()

        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD
        viewModel.refreshTransactionDisplaySettings()

        verify(exactly = 2) { transactionViewItemFactory.updateCache() }
    }

    @Test
    fun refreshTransactionDisplaySettings_calledTwiceWithoutChange_refreshesOnlyOnce() = runTest(dispatcher) {
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD

        val viewModel = createViewModel()
        advanceUntilIdle()
        clearMocks(transactionViewItemFactory, answers = false)

        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.COMPACT
        viewModel.refreshTransactionDisplaySettings()
        viewModel.refreshTransactionDisplaySettings()

        verify(exactly = 1) { transactionViewItemFactory.updateCache() }
    }

    @Test
    fun refreshTransactionDisplaySettings_modeChanged_reprocessesTransactions() = runTest(dispatcher) {
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.STANDARD

        transactionItemsFlow.value = listOf(
            createTransactionItem("tx-1"),
            createTransactionItem("tx-2")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.COMPACT

        viewModel.refreshTransactionDisplaySettings()

        verify(exactly = 1) { transactionViewItemFactory.updateCache() }
        assertEquals(2, viewModel.uiState.transactions?.values?.flatten()?.size)
    }

    // endregion

    // region Helper Methods

    private fun createViewModel(): TokenBalanceViewModel = TokenBalanceViewModel(
        totalBalance = totalBalance,
        wallet = testWallet,
        balanceService = balanceService,
        balanceViewItemFactory = balanceViewItemFactory,
        transactionsService = transactionsService,
        transactionViewItem2Factory = transactionViewItemFactory,
        balanceHiddenManager = balanceHiddenManager,
        connectivityManager = connectivityManager,
        accountManager = accountManager,
        transactionHiddenManager = transactionHiddenManager,
        getChangeNowAssociatedCoinTickerUseCase = getChangeNowAssociatedCoinTickerUseCase,
        premiumSettings = premiumSettings,
        amlStatusManager = amlStatusManager,
        marketFavoritesManager = marketFavoritesManager,
        stackingManager = stackingManager,
        priceManager = priceManager,
        localStorage = localStorage,
        numberFormatter = numberFormatter,
        contactsRepository = contactsRepository,
    )

    private fun createHiddenState(
        hidden: Boolean,
        level: TransactionDisplayLevel = TransactionDisplayLevel.LAST_2_TRANSACTIONS
    ) = TransactionHiddenState(
        transactionHidden = hidden,
        transactionHideEnabled = true,
        transactionDisplayLevel = level,
        transactionAutoHidePinExists = false
    )

    private fun createTestWallet(): Wallet {
        val testCoin = Coin(uid = "test-coin", name = "Test Coin", code = "TEST")
        val testToken = Token(
            coin = testCoin,
            blockchain = Blockchain(
                type = BlockchainType.Bitcoin,
                name = "Bitcoin",
                eip3091url = null
            ),
            type = TokenType.Native,
            decimals = 8
        )
        return mockk<Wallet>(relaxed = true) {
            every { token } returns testToken
            every { coin } returns testCoin
            every { tokenQueryId } returns testToken.tokenQuery.id
        }
    }

    private fun createTransactionItem(uid: String): TransactionItem {
        val record = mockk<TransactionRecord>(relaxed = true) {
            every { this@mockk.uid } returns uid
            every { timestamp } returns 0L
        }

        return TransactionItem(
            record = record,
            currencyValue = null,
            lastBlockInfo = null,
            nftMetadata = emptyMap()
        )
    }

    private fun createMockTransactionViewItem(uid: String) = mockk<TransactionViewItem>(relaxed = true) {
        every { this@mockk.uid } returns uid
        every { formattedDate } returns "DATE"
    }

    private fun createBalanceViewItem(
        secondaryValue: DeemedValue<String> = DeemedValue("", dimmed = false, visible = true)
    ) = BalanceViewItem(
        wallet = testWallet,
        primaryValue = DeemedValue("1.5 TEST", dimmed = false, visible = true),
        exchangeValue = DeemedValue("", dimmed = false, visible = false),
        secondaryValue = secondaryValue,
        lockedValues = emptyList(),
        sendEnabled = false,
        syncingProgress = SyncingProgress(null, null),
        syncingTextValue = null,
        syncedUntilTextValue = null,
        failedIconVisible = false,
        coinIconVisible = true,
        badge = null,
        swapVisible = false,
        swapEnabled = false,
        errorMessage = null,
        isWatchAccount = false,
        isSendDisabled = false,
        isShowShieldFunds = false,
        warning = null
    )

    private fun createPirateCashWallet(): Wallet {
        val pirateCoin = Coin(uid = "pirate-cash", name = "PirateCash", code = "PIRATE")
        val pirateToken = Token(
            coin = pirateCoin,
            blockchain = Blockchain(
                type = BlockchainType.BinanceSmartChain,
                name = "BNB Smart Chain",
                eip3091url = null
            ),
            type = TokenType.Eip20(cash.p.terminal.wallet.BuildConfig.PIRATE_CONTRACT),
            decimals = 18
        )
        val account = mockk<Account>(relaxed = true)
        val walletFactory = WalletFactory(mockk(relaxed = true))
        return walletFactory.create(pirateToken, account, null)!!
    }

    private fun createBep20Wallet(): Wallet {
        val coin = Coin(uid = "test-bep20", name = "TestBep20", code = "TBEP")
        val token = Token(
            coin = coin,
            blockchain = Blockchain(
                type = BlockchainType.BinanceSmartChain,
                name = "BNB Smart Chain",
                eip3091url = null
            ),
            type = TokenType.Eip20("0x1234567890abcdef1234567890abcdef12345678"),
            decimals = 18
        )
        val account = mockk<Account>(relaxed = true)
        val walletFactory = WalletFactory(mockk(relaxed = true))
        return walletFactory.create(token, account, null)!!
    }

    private fun createTrc20Wallet(): Wallet {
        val usdtCoin = Coin(uid = "tether", name = "Tether", code = "USDT")
        val usdtToken = Token(
            coin = usdtCoin,
            blockchain = Blockchain(
                type = BlockchainType.Tron,
                name = "TRON",
                eip3091url = null
            ),
            type = TokenType.Eip20("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"),
            decimals = 6
        )
        val account = mockk<Account>(relaxed = true)
        val walletFactory = WalletFactory(mockk(relaxed = true))
        return walletFactory.create(usdtToken, account, null)!!
    }

    private fun createBalanceItem(
        balance: BigDecimal = BigDecimal("1.5"),
        wallet: Wallet = testWallet
    ) = BalanceItem(
        wallet = wallet,
        balanceData = BalanceData(available = balance),
        state = AdapterState.Synced,
        sendAllowed = true,
        coinPrice = null
    )

    // endregion
}
