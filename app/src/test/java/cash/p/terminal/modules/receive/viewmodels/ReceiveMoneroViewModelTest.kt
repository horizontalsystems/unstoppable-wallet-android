package cash.p.terminal.modules.receive.viewmodels

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.adapters.MoneroAdapter
import cash.p.terminal.core.managers.MoneroSubaddressInfo
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.Coin
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiveMoneroViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val testDispatcherProvider = TestDispatcherProvider(dispatcher, testScope)

    private val adapter = mockk<MoneroAdapter>(relaxed = true)
    private val adapterManager = mockk<IAdapterManager>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)

    private val account = mockk<Account> {
        every { isWatchAccount } returns false
    }
    private val coin = mockk<Coin> {
        every { code } returns "XMR"
    }
    private val blockchain = mockk<Blockchain> {
        every { name } returns "Monero"
    }
    private val token = mockk<Token>(relaxed = true) {
        every { this@mockk.blockchain } returns this@ReceiveMoneroViewModelTest.blockchain
        every { blockchainType } returns BlockchainType.Monero
    }
    private val wallet = mockk<Wallet>(relaxed = true) {
        every { this@mockk.account } returns this@ReceiveMoneroViewModelTest.account
        every { this@mockk.coin } returns this@ReceiveMoneroViewModelTest.coin
        every { this@mockk.token } returns this@ReceiveMoneroViewModelTest.token
    }

    private val subaddress0 = MoneroSubaddressInfo(0, "addr_primary_0", 500L)
    private val subaddress1 = MoneroSubaddressInfo(1, "addr_sub_1", 0L)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { adapterManager.adaptersReadyObservable } returns Flowable.empty()
        every { adapterManager.getAdapterForWallet<MoneroAdapter>(wallet) } returns adapter
        coEvery { adapter.getSubaddresses() } returns listOf(subaddress0, subaddress1)
        every { localStorage.moneroSkipNewAddressConfirm } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ReceiveMoneroViewModel(
        wallet = wallet,
        adapterManager = adapterManager,
        localStorage = localStorage,
        dispatcherProvider = testDispatcherProvider,
    )

    @Test
    fun createNewAddress_setsNewBadge() = runTest(dispatcher) {
        coEvery { adapter.createNewSubaddress() } returns "addr_sub_2"
        val newSubaddress2 = MoneroSubaddressInfo(2, "addr_sub_2", 0L)
        coEvery { adapter.getSubaddresses() } returnsMany listOf(
            listOf(subaddress0, subaddress1),
            listOf(subaddress0, subaddress1, newSubaddress2),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.createNewAddress()
        advanceUntilIdle()

        assertEquals(AddressBadge.NEW, viewModel.uiState.addressBadge)
        assertEquals("addr_sub_2", viewModel.uiState.address)
    }

    @Test
    fun newBadge_resetsToUnused_onVmRecreation() = runTest(dispatcher) {
        coEvery { adapter.createNewSubaddress() } returns "addr_sub_2"
        val newSubaddress2 = MoneroSubaddressInfo(2, "addr_sub_2", 0L)
        coEvery { adapter.getSubaddresses() } returnsMany listOf(
            listOf(subaddress0, subaddress1),
            listOf(subaddress0, subaddress1, newSubaddress2),
            listOf(subaddress0, subaddress1, newSubaddress2),
        )
        val viewModel1 = createViewModel()
        advanceUntilIdle()

        viewModel1.createNewAddress()
        advanceUntilIdle()
        assertEquals(AddressBadge.NEW, viewModel1.uiState.addressBadge)

        val viewModel2 = createViewModel()
        advanceUntilIdle()

        assertEquals(AddressBadge.UNUSED, viewModel2.uiState.addressBadge)
        assertEquals("addr_sub_2", viewModel2.uiState.address)
    }

    @Test
    fun fetchData_addressWithReceivedAmount_showsUsedBadge() = runTest(dispatcher) {
        coEvery { adapter.getSubaddresses() } returns listOf(
            subaddress0,
            MoneroSubaddressInfo(1, "addr_sub_1", 100L),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(AddressBadge.USED, viewModel.uiState.addressBadge)
    }

    @Test
    fun fetchData_addressWithNoReceivedAmount_showsUnusedBadge() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(AddressBadge.UNUSED, viewModel.uiState.addressBadge)
    }

    @Test
    fun hasAddressHistory_falseWhenOnlyOneSubaddress() = runTest(dispatcher) {
        coEvery { adapter.getSubaddresses() } returns listOf(subaddress0)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.hasAddressHistory)
    }

    @Test
    fun hasAddressHistory_trueWhenMultipleSubaddresses() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.hasAddressHistory)
    }
}
