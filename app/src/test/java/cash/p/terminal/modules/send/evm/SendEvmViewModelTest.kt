package cash.p.terminal.modules.send.evm

import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.ServiceStateFlow
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceEvm
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
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
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import org.koin.test.KoinTestRule
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SendEvmViewModelTest : KoinTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val adapter = mockk<ISendEthereumAdapter>(relaxed = true)
    private val sendTransactionService = mockk<SendTransactionServiceEvm>(relaxed = true)
    private val xRateService = mockk<XRateService>(relaxed = true)
    private val amountService = mockk<SendAmountService>(relaxed = true)
    private val addressService = mockk<SendEvmAddressService>(relaxed = true)
    private val evmBlockchainManager = mockk<EvmBlockchainManager>()
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val balanceHiddenManager = mockk<IBalanceHiddenManager>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)

    private lateinit var amountStateFlow: MutableStateFlow<SendAmountService.State>
    private lateinit var addressStateFlow: MutableStateFlow<SendEvmAddressService.State>
    private lateinit var transactionSharedFlow: MutableSharedFlow<SendTransactionServiceState>
    private lateinit var transactionStateFlow: ServiceStateFlow<SendTransactionServiceState>

    private val testAddress = Address("0xafcc12e4040615e7afe9fb4330eb3d9120acac05")
    private val testAmount = BigDecimal("0.001")
    private val testCoin = Coin(uid = "test-coin", name = "BNB", code = "BNB")
    private val testToken = Token(
        coin = testCoin,
        blockchain = Blockchain(
            type = BlockchainType.BinanceSmartChain,
            name = "BNB Smart Chain",
            eip3091url = null
        ),
        type = TokenType.Native,
        decimals = 18
    )
    private val testWallet = mockk<Wallet>(relaxed = true) {
        every { token } returns testToken
        every { coin } returns testCoin
    }
    private val testTransactionData = TransactionData(
        to = io.horizontalsystems.ethereumkit.models.Address(testAddress.hex),
        value = BigDecimal.ZERO.toBigInteger(),
        input = ByteArray(0)
    )

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single { evmBlockchainManager }
                single { mockk<ContactsRepository>(relaxed = true) }
                single<IBalanceHiddenManager> { balanceHiddenManager }
                single<MarketKitWrapper> { marketKit }
            }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        amountStateFlow = MutableStateFlow(createAmountState())
        addressStateFlow = MutableStateFlow(createAddressState())

        transactionSharedFlow = MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        transactionSharedFlow.tryEmit(createTransactionServiceState())
        transactionStateFlow = ServiceStateFlow(transactionSharedFlow)

        every { amountService.stateFlow } returns amountStateFlow
        every { addressService.stateFlow } returns addressStateFlow
        every { sendTransactionService.stateFlow } returns transactionStateFlow
        every { xRateService.getRate(any()) } returns null
        every { xRateService.getRateFlow(any()) } returns flowOf<CurrencyValue>()
        every { evmBlockchainManager.getBaseToken(BlockchainType.BinanceSmartChain) } returns testToken
        every { balanceHiddenManager.balanceHiddenFlow } returns MutableStateFlow(false)
        every { adapter.getTransactionData(any(), any()) } returns testTransactionData

        every { amountService.setAmount(any()) } answers {
            val amount = firstArg<BigDecimal?>()
            amountStateFlow.value = createAmountState(
                amount = amount,
                canBeSend = amount != null && amount > BigDecimal.ZERO
            )
        }
        every { addressService.setAddress(any()) } answers {
            val address = firstArg<Address?>()
            addressStateFlow.value = createAddressState(
                address = address,
                canBeSend = address != null
            )
        }
        coEvery { sendTransactionService.setSendTransactionData(any()) } answers {
            transactionSharedFlow.tryEmit(createTransactionServiceState(sendable = true))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun canBeSend_amountFirstThenAddress_becomesTrue() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnterAmount(testAmount)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.canBeSend)

        viewModel.onEnterAddress(testAddress)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.canBeSend)
    }

    @Test
    fun canBeSend_addressFirstThenAmount_becomesTrue() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnterAddress(testAddress)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.canBeSend)

        viewModel.onEnterAmount(testAmount)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.canBeSend)
    }

    @Test
    fun canBeSend_onlyAmount_staysFalse() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnterAmount(testAmount)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.canBeSend)
    }

    @Test
    fun canBeSend_onlyAddress_staysFalse() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnterAddress(testAddress)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.canBeSend)
    }

    @Test
    fun updateSendTransactionData_amountChanges_recalculatesWithNewAmount() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEnterAddress(testAddress)
            viewModel.onEnterAmount(testAmount)
            advanceUntilIdle()

            val newAmount = BigDecimal("0.005")
            viewModel.onEnterAmount(newAmount)
            advanceUntilIdle()

            verify(atLeast = 1) {
                adapter.getTransactionData(eq(newAmount), any())
            }
        }

    @Test
    fun updateSendTransactionData_addressChanges_recalculatesWithNewAddress() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEnterAmount(testAmount)
            viewModel.onEnterAddress(testAddress)
            advanceUntilIdle()

            val newAddress = Address("0x1234567890abcdef1234567890abcdef12345678")
            viewModel.onEnterAddress(newAddress)
            advanceUntilIdle()

            verify(atLeast = 1) {
                adapter.getTransactionData(
                    any(),
                    eq(io.horizontalsystems.ethereumkit.models.Address(newAddress.hex))
                )
            }
        }

    @Test
    fun canBeSend_transactionServiceNotSendable_staysFalse() = runTest(dispatcher) {
        coEvery { sendTransactionService.setSendTransactionData(any()) } answers {
            transactionSharedFlow.tryEmit(createTransactionServiceState(sendable = false))
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnterAmount(testAmount)
        viewModel.onEnterAddress(testAddress)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.canBeSend)
    }

    private fun createViewModel() = SendEvmViewModel(
        wallet = testWallet,
        sendToken = testToken,
        adapter = adapter,
        sendTransactionService = sendTransactionService,
        xRateService = xRateService,
        amountService = amountService,
        addressService = addressService,
        coinMaxAllowedDecimals = 18,
        showAddressInput = true,
        address = null,
        adapterManager = adapterManager
    )

    private fun createAmountState(
        amount: BigDecimal? = null,
        canBeSend: Boolean = false
    ) = SendAmountService.State(
        amount = amount,
        amountCaution = null,
        availableBalance = BigDecimal.TEN,
        canBeSend = canBeSend
    )

    private fun createAddressState(
        address: Address? = null,
        canBeSend: Boolean = false
    ) = SendEvmAddressService.State(
        address = address,
        evmAddress = address?.let {
            io.horizontalsystems.ethereumkit.models.Address(it.hex)
        },
        addressError = null,
        canBeSend = canBeSend
    )

    private fun createTransactionServiceState(
        sendable: Boolean = false
    ) = SendTransactionServiceState(
        availableBalance = BigDecimal.TEN,
        networkFee = null,
        cautions = emptyList(),
        sendable = sendable,
        loading = false,
        fields = emptyList()
    )
}
