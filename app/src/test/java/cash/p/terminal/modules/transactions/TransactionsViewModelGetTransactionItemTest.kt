package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.usecase.SyncPendingMultiSwapUseCase
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.transaction.TransactionSource
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.p.terminal.wallet.managers.TransactionHiddenState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelGetTransactionItemTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { mockk<UpdateSwapProviderTransactionsStatusUseCase>(relaxed = true) }
                single { mockk<SyncPendingMultiSwapUseCase>(relaxed = true) }
                single {
                    mockk<PoisonAddressManager>(relaxed = true) {
                        every { poisonDbChangedFlow } returns MutableSharedFlow()
                    }
                }
            }
        )
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(dispatcher)

    private val service = mockk<TransactionsService>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val walletManager = mockk<IWalletManager>(relaxed = true)
    private val swapProviderTransactionsStorage = mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val balanceHiddenManager = mockk<BalanceHiddenManager>(relaxed = true)
    private val amlStatusManager = mockk<AmlStatusManager>(relaxed = true)
    private val premiumSettings = mockk<PremiumSettings>(relaxed = true)
    private val transactionHiddenManager = mockk<TransactionHiddenManager>(relaxed = true)
    private val transactionFilterService = mockk<TransactionFilterService>(relaxed = true)
    private val contactsRepository = mockk<ContactsRepository>(relaxed = true)

    private val viewModel by lazy {
        TransactionsViewModel(
            service = service,
            transactionViewItem2Factory = mockk(relaxed = true),
            balanceHiddenManager = balanceHiddenManager,
            transactionAdapterManager = transactionAdapterManager,
            walletManager = walletManager,
            transactionFilterService = transactionFilterService,
            transactionHiddenManager = transactionHiddenManager,
            premiumSettings = premiumSettings,
            amlStatusManager = amlStatusManager,
            adapterManager = mockk(relaxed = true),
            swapProviderTransactionsStorage = swapProviderTransactionsStorage,
            contactsRepository = contactsRepository,
            dispatcherProvider = TestDispatcherProvider(dispatcher, testScope),
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { service.transactionItemsFlow } returns MutableStateFlow(emptyList())
        every { service.syncingFlow } returns MutableStateFlow(false)
        every { transactionFilterService.stateFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { transactionHiddenManager.transactionHiddenFlow } returns MutableStateFlow(
            TransactionHiddenState(false, false, TransactionDisplayLevel.NOTHING, false)
        )
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
        every { transactionAdapterManager.adaptersReadyFlow } returns MutableStateFlow(emptyMap())
        every { transactionAdapterManager.initializationFlow } returns MutableStateFlow(false)
        every { walletManager.activeWalletsFlow } returns MutableStateFlow(emptyList())
        every { walletManager.activeWallets } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun transactionItem(uid: String) = TransactionItem(
        record = mockk<TransactionRecord>(relaxed = true) { every { this@mockk.uid } returns uid },
        currencyValue = null,
        lastBlockInfo = null,
        nftMetadata = emptyMap(),
    )

    private fun swapProviderTx(
        provider: SwapProvider = SwapProvider.CHANGENOW,
        transactionId: String = "cn-tx-123",
        addressOut: String = "0xRecipient",
    ) = SwapProviderTransaction(
        date = 1000L,
        outgoingRecordUid = "outgoing-uid",
        transactionId = transactionId,
        status = "finished",
        provider = provider,
        coinUidIn = "bitcoin",
        blockchainTypeIn = "bitcoin",
        amountIn = BigDecimal.ONE,
        addressIn = "0xSender",
        coinUidOut = "tether",
        blockchainTypeOut = "ethereum",
        amountOut = BigDecimal("100"),
        addressOut = addressOut,
    )

    @Test
    fun getTransactionItem_offChainOutgoingRecordUid_enrichesSwapMetadata() = runTest(dispatcher) {
        val item = transactionItem("outgoing-uid")
        val swapTx = swapProviderTx()

        every { service.getTransactionItem("outgoing-uid") } returns item
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("outgoing-uid") } returns swapTx

        val result = viewModel.getTransactionItem("outgoing-uid")

        assertNotNull(result)
        assertEquals("cn-tx-123", result?.changeNowTransactionId)
        assertNotNull(result?.transactionStatusUrl)
        assertEquals("changenow.io", result?.transactionStatusUrl?.first)
    }

    @Test
    fun getTransactionItem_offChainIncomingRecordUid_enrichesSwapMetadata() = runTest(dispatcher) {
        val item = transactionItem("incoming-uid")
        val swapTx = swapProviderTx(
            provider = SwapProvider.QUICKEX,
            transactionId = "qx-order-456",
            addressOut = "0xDest",
        )

        every { service.getTransactionItem("incoming-uid") } returns item
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("incoming-uid") } returns null
        every { swapProviderTransactionsStorage.getByIncomingRecordUid("incoming-uid") } returns swapTx

        val result = viewModel.getTransactionItem("incoming-uid")

        assertNotNull(result)
        assertEquals("qx-order-456", result?.changeNowTransactionId)
        assertNotNull(result?.transactionStatusUrl)
        assertEquals("quickex.io", result?.transactionStatusUrl?.first)
    }

    @Test
    fun getTransactionItem_noSwapProviderTx_returnsPlainItem() = runTest(dispatcher) {
        val item = transactionItem("plain-uid")

        every { service.getTransactionItem("plain-uid") } returns item
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("plain-uid") } returns null
        every { swapProviderTransactionsStorage.getByIncomingRecordUid("plain-uid") } returns null

        val result = viewModel.getTransactionItem("plain-uid")

        assertNotNull(result)
        assertNull(result?.changeNowTransactionId)
        assertNull(result?.transactionStatusUrl)
    }

    @Test
    fun getTransactionItem_notInServiceCache_fallsBackToAdapter() = runTest(dispatcher) {
        val record = mockk<TransactionRecord>(relaxed = true) {
            every { uid } returns "adapter-uid"
        }
        val adapter = mockk<ITransactionsAdapter>(relaxed = true)
        coEvery { adapter.getTransactions(any(), any(), any(), any(), any()) } returns listOf(record)

        val source = mockk<TransactionSource>(relaxed = true)
        every { transactionAdapterManager.adaptersReadyFlow } returns MutableStateFlow(mapOf(source to adapter))
        every { service.getTransactionItem("adapter-uid") } returns null
        every { swapProviderTransactionsStorage.getByOutgoingRecordUid("adapter-uid") } returns null
        every { swapProviderTransactionsStorage.getByIncomingRecordUid("adapter-uid") } returns null

        val result = viewModel.getTransactionItem("adapter-uid")

        assertNotNull(result)
        assertEquals("adapter-uid", result?.record?.uid)
    }

    @Test
    fun getTransactionItem_notInServiceOrAdapters_returnsNull() = runTest(dispatcher) {
        val adapter = mockk<ITransactionsAdapter>(relaxed = true)
        coEvery { adapter.getTransactions(any(), any(), any(), any(), any()) } returns emptyList()

        val source = mockk<TransactionSource>(relaxed = true)
        every { transactionAdapterManager.adaptersReadyFlow } returns MutableStateFlow(mapOf(source to adapter))
        every { service.getTransactionItem("missing-uid") } returns null

        val result = viewModel.getTransactionItem("missing-uid")

        assertNull(result)
    }
}
