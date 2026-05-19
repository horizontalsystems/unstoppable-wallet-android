package cash.p.terminal.modules.send.bitcoin

import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class SendBitcoinViewModelTest {
    private val mainScheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(mainScheduler)
    private val defaultScheduler = TestCoroutineScheduler()
    private val defaultDispatcher = StandardTestDispatcher(defaultScheduler)
    private val balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val adapter = mockk<SendBalanceAdapter>(relaxed = true)
    private val feeRateService = mockk<SendBitcoinFeeRateService>(relaxed = true)
    private val feeService = mockk<SendBitcoinFeeService>(relaxed = true)
    private val amountService = mockk<SendBitcoinAmountService>(relaxed = true)
    private val addressService = mockk<SendBitcoinAddressService>(relaxed = true)
    private val pluginService = mockk<SendBitcoinPluginService>(relaxed = true)
    private val xRateService = mockk<XRateService>(relaxed = true)
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val dispatcherProvider = SplitDispatcherProvider(mainDispatcher, defaultDispatcher)
    private val wallet = WalletFactory.previewWallet()
    private lateinit var amountStateFlow: MutableStateFlow<SendBitcoinAmountService.State>
    private lateinit var addressStateFlow: MutableStateFlow<SendBitcoinAddressService.State>

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { mockk<MarketKitWrapper>(relaxed = true) }
                single<IBalanceHiddenManager> {
                    mockk(relaxed = true) {
                        every { balanceHiddenFlow } returns MutableStateFlow(false)
                    }
                }
                single { mockk<PoisonAddressManager>(relaxed = true) }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)

        every { adapter.balanceUpdatedFlow } returns balanceUpdatedFlow
        every { adapter.balanceState } returns AdapterState.Synced
        every { adapter.balanceStateUpdatedFlow } returns emptyFlow()
        every { adapter.blockchainType } returns BlockchainType.Bitcoin
        every { adapterManager.getBalanceAdapterForWallet(wallet) } returns null

        every { feeRateService.stateFlow } returns MutableStateFlow(
            SendBitcoinFeeRateService.State(
                feeRate = null,
                feeRateCaution = null,
                canBeSend = false,
                isRecommended = false
            )
        )
        coEvery { feeRateService.start() } returns Unit

        amountStateFlow = MutableStateFlow(
            SendBitcoinAmountService.State(
                amount = null,
                amountCaution = null,
                availableBalance = null,
                canBeSend = false
            )
        )
        every { amountService.stateFlow } returns amountStateFlow
        addressStateFlow = MutableStateFlow(
            SendBitcoinAddressService.State(
                validAddress = null,
                addressError = null,
                canBeSend = false
            )
        )
        every { addressService.stateFlow } returns addressStateFlow
        every { pluginService.stateFlow } returns MutableStateFlow(
            SendBitcoinPluginService.State(
                lockTimeInterval = null,
                pluginData = null
            )
        )
        every { pluginService.isLockTimeEnabled } returns false
        every { pluginService.lockTimeIntervals } returns emptyList()
        every { feeService.bitcoinFeeInfoFlow } returns MutableStateFlow(null)
        every { xRateService.getRate(wallet.coin.uid) } returns null
        every { xRateService.getRateFlow(wallet.coin.uid) } returns emptyFlow<CurrencyValue>()
        every { localStorage.utxoExpertModeEnabled } returns false
        every { localStorage.utxoExpertModeEnabledFlow } returns MutableStateFlow(false)
        justRun { addressService.setAddress(null) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun balanceUpdatedFlow_emits_refreshesBalanceAndFeeOnDefaultDispatcher() = runTest(mainDispatcher) {
        createViewModel()
        advanceUntilIdle()

        balanceUpdatedFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 0) { amountService.refreshAvailableBalance() }
        verify(exactly = 0) { feeService.refresh() }

        defaultScheduler.advanceUntilIdle()

        verify(exactly = 1) { amountService.refreshAvailableBalance() }
        verify(exactly = 1) { feeService.refresh() }
    }

    @Test
    fun getConfirmationData_missingAddress_throwsClearError() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val error = assertFailsWith<LocalizedException> {
            viewModel.getConfirmationData()
        }

        assertEquals(R.string.send_error_address_unavailable, error.errorTextRes)
    }

    @Test
    fun getConfirmationData_missingAmount_throwsClearError() = runTest(mainDispatcher) {
        addressStateFlow.value = SendBitcoinAddressService.State(
            validAddress = Address("bc1qrecipient"),
            addressError = null,
            canBeSend = true
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        val error = assertFailsWith<LocalizedException> {
            viewModel.getConfirmationData()
        }

        assertEquals(R.string.send_error_amount_unavailable, error.errorTextRes)
    }

    private fun createViewModel() = SendBitcoinViewModel(
        adapter = adapter,
        wallet = wallet,
        feeRateService = feeRateService,
        feeService = feeService,
        amountService = amountService,
        addressService = addressService,
        pluginService = pluginService,
        xRateService = xRateService,
        btcBlockchainManager = mockk<BtcBlockchainManager>(relaxed = true),
        contactsRepo = mockk<ContactsRepository>(relaxed = true),
        showAddressInput = true,
        localStorage = localStorage,
        address = null,
        pendingRegistrar = mockk<PendingTransactionRegistrar>(relaxed = true),
        adapterManager = adapterManager,
        dispatcherProvider = dispatcherProvider
    )

    private interface SendBalanceAdapter : ISendBitcoinAdapter, IBalanceAdapter

    private class SplitDispatcherProvider(
        override val main: CoroutineDispatcher,
        override val default: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val io: CoroutineDispatcher = main
        override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + io)
    }
}
