package cash.p.terminal.modules.multiswap.sendtransaction.services

import cash.p.terminal.core.App
import cash.p.terminal.core.ISendTonAdapter
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.entities.PendingTransactionDraft
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.tonkit.Address
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SendTransactionServiceTonSwapTest : KoinTest {

    private lateinit var pendingRegistrar: PendingTransactionRegistrar
    private lateinit var adapter: ISendTonAdapter
    private lateinit var walletUseCase: WalletUseCase
    private lateinit var adapterManager: IAdapterManager
    private lateinit var marketKit: MarketKitWrapper
    private lateinit var numberFormatter: IAppNumberFormatter
    private lateinit var currencyManager: CurrencyManager
    private lateinit var accountManager: IAccountManager

    private lateinit var testToken: Token
    private lateinit var testWallet: Wallet

    private val testBalance = BigDecimal("100")

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(
            module {
                single<PendingTransactionRegistrar> { pendingRegistrar }
                single<WalletUseCase> { walletUseCase }
                single<IAdapterManager> { adapterManager }
                single<MarketKitWrapper> { marketKit }
                single<IAppNumberFormatter> { numberFormatter }
                single<CurrencyManager> { currencyManager }
                single<IAccountManager> { accountManager }
            }
        )
    }

    @Before
    fun setUp() {
        pendingRegistrar = mockk(relaxed = true)
        adapter = mockk(relaxed = true)
        walletUseCase = mockk(relaxed = true)
        adapterManager = mockk(relaxed = true)
        marketKit = mockk(relaxed = true)
        numberFormatter = mockk(relaxed = true)
        currencyManager = mockk(relaxed = true)
        accountManager = mockk(relaxed = true)

        setupTestData()
        setupMocks()
        setupAppMocks()
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun sendTransaction_tonSwap_registersPendingDraftWithRouterAddress() = runTest {
        val draftSlot = slot<PendingTransactionDraft>()
        coEvery { pendingRegistrar.register(capture(draftSlot)) } returns "pending-tx-id"

        val service = SendTransactionServiceTonSwap(testToken)
        val data = tonSwapData()
        service.setSendTransactionData(data)

        service.sendTransaction()

        val capturedDraft = draftSlot.captured
        assertEquals(data.routerAddress, capturedDraft.toAddress)
        assertEquals(BigDecimal("0.574700000"), capturedDraft.amount)
        assertEquals(testBalance, capturedDraft.sdkBalanceAtCreation)
        assertEquals("", capturedDraft.fromAddress)
        assertEquals(testWallet, capturedDraft.wallet)
        assertEquals(testToken, capturedDraft.token)
        assertNull(capturedDraft.fee)
        assertNull(capturedDraft.txHash)
        coVerify { adapter.sendWithPayload(any(), data.destinationAddress.orEmpty(), data.payload) }
        coVerify(exactly = 0) { pendingRegistrar.updateTxId(any(), any()) }
    }

    private fun setupTestData() {
        val testCoin = Coin(uid = "toncoin", name = "The Open Network", code = "TON")
        testToken = Token(
            coin = testCoin,
            blockchain = Blockchain(
                type = BlockchainType.Ton,
                name = "The Open Network",
                eip3091url = null
            ),
            type = TokenType.Native,
            decimals = 9
        )

        testWallet = mockk(relaxed = true) {
            every { token } returns testToken
            every { coin } returns testCoin
        }
    }

    private fun setupMocks() {
        every { adapter.balanceData } returns BalanceData(testBalance)
        every { adapter.maxSpendableBalance } returns testBalance
        every { currencyManager.baseCurrency } returns Currency("USD", "$", 2, 0)
        every { marketKit.token(any()) } returns testToken
        every { marketKit.coinPrice(any(), any()) } returns null

        coEvery { walletUseCase.createWalletIfNotExists(any()) } returns testWallet
        coEvery { adapterManager.awaitAdapterForWallet<ISendTonAdapter>(any(), any()) } returns adapter
        every { adapterManager.getBalanceAdapterForWallet(any()) } returns adapter
        every { adapterManager.getAdjustedBalanceData(any()) } returns BalanceData(testBalance)
        every { adapterManager.getAdjustedBalanceDataForToken(any()) } returns BalanceData(testBalance)
    }

    private fun setupAppMocks() {
        mockkObject(App)
        every { App.currencyManager } returns currencyManager
        every { App.marketKit } returns marketKit
    }

    private fun tonSwapData(): SendTransactionData.TonSwap {
        return SendTransactionData.TonSwap(
            forwardGas = BigInteger("300000000"),
            offerUnits = BigInteger("574700000"),
            routerAddress = TON_ROUTER_RAW,
            routerMasterAddress = TON_ROUTER_RAW,
            destinationAddress = Address.parse(TON_DESTINATION_RAW).toUserFriendly(bounceable = true),
            queryId = 123L,
            slippage = BigDecimal("0.005"),
            payload = "te6ccgEBAQEAAgAAAA==",
            gasBudget = null,
        )
    }

    private companion object {
        const val TON_ROUTER_RAW =
            "0:1111111111111111111111111111111111111111111111111111111111111111"
        const val TON_DESTINATION_RAW =
            "0:2222222222222222222222222222222222222222222222222222222222222222"
    }
}
