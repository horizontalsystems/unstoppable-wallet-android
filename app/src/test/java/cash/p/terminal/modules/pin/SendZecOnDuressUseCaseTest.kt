package cash.p.terminal.modules.pin

import android.content.Context
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.managers.LocallyCreatedTransactionRepository
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAccountsStorage
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.ISmsNotificationSettings
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigDecimal
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SendZecOnDuressUseCaseTest {

    @MockK
    private lateinit var smsNotificationSettings: ISmsNotificationSettings

    @MockK
    private lateinit var accountsStorage: IAccountsStorage

    @MockK
    private lateinit var walletManager: IWalletManager

    @MockK
    private lateinit var dispatcherProvider: DispatcherProvider

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var localStorage: ILocalStorage

    @MockK
    private lateinit var backgroundManager: BackgroundManager

    @MockK
    private lateinit var restoreSettingsManager: RestoreSettingsManager

    @MockK
    private lateinit var adapterManager: IAdapterManager

    @MockK
    private lateinit var coinManager: ICoinManager

    @MockK
    private lateinit var walletFactory: WalletFactory

    @MockK
    private lateinit var clearZCashWalletDataUseCase: ClearZCashWalletDataUseCase

    @MockK
    private lateinit var accountManager: IAccountManager

    @MockK(relaxed = true)
    private lateinit var locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository

    private lateinit var testScope: TestScope
    private lateinit var useCase: SendZecOnDuressUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testScope = TestScope()
        every { dispatcherProvider.applicationScope } returns testScope
        startKoin {
            modules(
                module {
                    single { locallyCreatedTransactionRepository }
                }
            )
        }

        useCase = SendZecOnDuressUseCase(
            smsNotificationSettings = smsNotificationSettings,
            accountsStorage = accountsStorage,
            walletManager = walletManager,
            dispatcherProvider = dispatcherProvider,
            context = context,
            localStorage = localStorage,
            backgroundManager = backgroundManager,
            restoreSettingsManager = restoreSettingsManager,
            adapterManager = adapterManager,
            coinManager = coinManager,
            walletFactory = walletFactory,
            clearZCashWalletDataUseCase = clearZCashWalletDataUseCase,
            accountManager = accountManager,
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ==================== sendIfEnabled Early Exit Tests ====================

    @Test
    fun sendIfEnabled_userLevel0_doesNothing() {
        useCase.sendIfEnabled(userLevel = 0)
        testScope.advanceUntilIdle()

        verify(exactly = 0) { smsNotificationSettings.getSmsNotificationAccountId(any()) }
    }

    @Test
    fun sendIfEnabled_userLevelNegative_doesNothing() {
        useCase.sendIfEnabled(userLevel = -1)
        testScope.advanceUntilIdle()

        verify(exactly = 0) { smsNotificationSettings.getSmsNotificationAccountId(any()) }
    }

    @Test
    fun sendIfEnabled_accountIdNull_doesNothing() {
        every { smsNotificationSettings.getSmsNotificationAccountId(0) } returns null

        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()

        verify { smsNotificationSettings.getSmsNotificationAccountId(0) }
        verify(exactly = 0) { smsNotificationSettings.getSmsNotificationAddress(any()) }
    }

    @Test
    fun sendIfEnabled_addressNull_doesNothing() {
        every { smsNotificationSettings.getSmsNotificationAccountId(0) } returns "account-123"
        every { smsNotificationSettings.getSmsNotificationAddress(0) } returns null

        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()

        verify { smsNotificationSettings.getSmsNotificationAddress(0) }
        coVerify(exactly = 0) { accountsStorage.loadAccount(any()) }
    }

    // ==================== sendIfEnabled Transaction Flow Tests ====================

    @Test
    fun sendIfEnabled_allConditionsMet_sendsTransaction() {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubSmsNotificationSettings(level = 0)
        stubWalletLookup(account, wallet)
        stubExistingAdapter(wallet, adapter)

        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()

        coVerify { adapter.send(any(), "z1testaddress", "test memo", null) }
    }

    @Test
    fun sendIfEnabled_memoNull_sendsWithEmptyMemo() {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        every { smsNotificationSettings.getSmsNotificationAccountId(0) } returns "account-123"
        every { smsNotificationSettings.getSmsNotificationAddress(0) } returns "z1testaddress"
        every { smsNotificationSettings.getSmsNotificationMemo(0) } returns null
        stubWalletLookup(account, wallet)
        stubExistingAdapter(wallet, adapter)

        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()

        coVerify { adapter.send(any(), "z1testaddress", "", null) }
    }

    @Test
    fun sendIfEnabled_exceptionDuringSend_doesNotCrash() {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)
        coEvery { adapter.send(any(), any(), any(), any()) } throws RuntimeException("Send failed")

        stubSmsNotificationSettings(level = 0)
        stubWalletLookup(account, wallet)
        stubExistingAdapter(wallet, adapter)

        // Should not throw - exception is caught internally
        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()
    }

    @Test
    fun sendIfEnabled_walletNotFound_doesNotCrash() {
        every { smsNotificationSettings.getSmsNotificationAccountId(0) } returns "account-123"
        every { smsNotificationSettings.getSmsNotificationAddress(0) } returns "z1testaddress"
        every { smsNotificationSettings.getSmsNotificationMemo(0) } returns "memo"
        coEvery { accountsStorage.loadAccount("account-123") } returns null

        // Should not throw - WalletNotFound is handled gracefully
        useCase.sendIfEnabled(userLevel = 1)
        testScope.advanceUntilIdle()

        // Verify wallet lookup was attempted
        coVerify { accountsStorage.loadAccount("account-123") }
    }

    // ==================== sendTestTransaction Tests ====================

    @Test
    fun sendTestTransaction_insufficientBalance_returnsInsufficientBalance() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("0.00001"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubExistingAdapter(wallet, adapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.InsufficientBalance, result)
    }

    @Test
    fun sendTestTransaction_sendSucceeds_returnsSuccess() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubExistingAdapter(wallet, adapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
    }

    @Test
    fun sendTestTransaction_sendThrows_returnsTransactionFailed() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)
        coEvery { adapter.send(any(), any(), any(), any()) } throws RuntimeException("Network error")

        stubWalletLookupForTestTransaction(account, wallet)
        stubExistingAdapter(wallet, adapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.TransactionFailed("Network error"), result)
    }

    // ==================== Adapter Selection Tests ====================

    @Test
    fun sendTransaction_onlyShieldedExists_usesShielded() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val shieldedAdapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubSingleExistingAdapter(wallet, shieldedAdapter, TokenType.AddressSpecType.Shielded)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
        coVerify { shieldedAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun sendTransaction_onlyUnifiedExists_usesUnified() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val unifiedAdapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubSingleExistingAdapter(wallet, unifiedAdapter, TokenType.AddressSpecType.Unified)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
        coVerify { unifiedAdapter.send(any(), any(), any(), any()) }
    }

    // ==================== Sync Racing Tests ====================

    @Test
    fun raceAdapters_allSyncedNoBalance_returnsInsufficientBalance() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val shieldedAdapter = createMockAdapter(balance = BigDecimal("0.00001"), synced = true)
        val unifiedAdapter = createMockAdapter(balance = BigDecimal("0.00002"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubBothExistingAdapters(wallet, shieldedAdapter, unifiedAdapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.InsufficientBalance, result)
        // Verify neither adapter was used to send
        coVerify(exactly = 0) { shieldedAdapter.send(any(), any(), any(), any()) }
        coVerify(exactly = 0) { unifiedAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun raceAdapters_firstHasBalance_usesFirstAdapter() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val shieldedAdapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)
        val unifiedAdapter = createMockAdapter(balance = BigDecimal("0.00001"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubBothExistingAdapters(wallet, shieldedAdapter, unifiedAdapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
        // Verify winner (shielded) was used
        coVerify(exactly = 1) { shieldedAdapter.send(any(), any(), any(), any()) }
        coVerify(exactly = 0) { unifiedAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun raceAdapters_secondHasBalance_usesSecondAdapter() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val shieldedAdapter = createMockAdapter(balance = BigDecimal("0.00001"), synced = true)
        val unifiedAdapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubBothExistingAdapters(wallet, shieldedAdapter, unifiedAdapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
        // Verify winner (unified) was used
        coVerify(exactly = 0) { shieldedAdapter.send(any(), any(), any(), any()) }
        coVerify(exactly = 1) { unifiedAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun raceAdapters_bothHaveBalance_usesOneAdapter() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val shieldedAdapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)
        val unifiedAdapter = createMockAdapter(balance = BigDecimal("2.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubBothExistingAdapters(wallet, shieldedAdapter, unifiedAdapter)

        val result = useCase.sendTestTransaction(wallet, "z1address", "memo")

        assertEquals(SendZecResult.Success, result)
        // Verify exactly one adapter was used (the winner)
        val shieldedCalled = try { coVerify(exactly = 1) { shieldedAdapter.send(any(), any(), any(), any()) }; true } catch (e: AssertionError) { false }
        val unifiedCalled = try { coVerify(exactly = 1) { unifiedAdapter.send(any(), any(), any(), any()) }; true } catch (e: AssertionError) { false }
        // Exactly one should be called
        assert(shieldedCalled xor unifiedCalled) { "Exactly one adapter should be used to send" }
    }

    // ==================== Cleanup Tests ====================

    @Test
    fun cleanup_existingAdapters_doesNotStopThem() = runTest {
        val account = createTestAccount()
        val wallet = createTestWallet(account)
        val adapter = createMockAdapter(balance = BigDecimal("1.0"), synced = true)

        stubWalletLookupForTestTransaction(account, wallet)
        stubExistingAdapter(wallet, adapter)

        useCase.sendTestTransaction(wallet, "z1address", "memo")

        // Existing adapters should NOT be stopped (they are managed externally)
        verify(exactly = 0) { adapter.stop() }
    }

    // ==================== Helper Functions ====================

    private fun createTestAccount(id: String = "account-123"): Account {
        return Account(
            id = id,
            name = "Test Account",
            type = AccountType.Mnemonic(
                words = List(12) { "word$it" },
                passphrase = ""
            ),
            origin = AccountOrigin.Created,
            level = 0,
            isBackedUp = true,
            isFileBackedUp = true
        )
    }

    private fun createTestToken(addressType: TokenType.AddressSpecType): Token {
        return Token(
            coin = Coin(
                uid = "zcash",
                name = "Zcash",
                code = "ZEC"
            ),
            blockchain = Blockchain(
                type = BlockchainType.Zcash,
                name = "Zcash",
                eip3091url = null
            ),
            type = TokenType.AddressSpecTyped(addressType),
            decimals = 8
        )
    }

    private fun createTestWallet(account: Account): Wallet {
        val token = createTestToken(TokenType.AddressSpecType.Shielded)
        return mockk<Wallet> {
            every { this@mockk.token } returns token
            every { this@mockk.account } returns account
        }
    }

    private fun createMockAdapter(
        balance: BigDecimal = BigDecimal("1.0"),
        synced: Boolean = true
    ): ISendZcashAdapter {
        val syncFlow = MutableSharedFlow<Unit>(replay = 1)
        val feeFlow = MutableStateFlow(BigDecimal("0.0001"))
        return mockk<ISendZcashAdapter> {
            every { maxSpendableBalance } returns balance
            every { balanceState } returns if (synced) AdapterState.Synced else AdapterState.Syncing()
            every { balanceStateUpdatedFlow } returns syncFlow
            every { fee } returns feeFlow
            every { start() } just runs
            every { stop() } just runs
            coEvery { send(any(), any(), any(), any()) } returns FirstClassByteArray(ByteArray(32) { it.toByte() })
        }.also {
            if (synced) syncFlow.tryEmit(Unit)
        }
    }

    private fun stubSmsNotificationSettings(level: Int) {
        every { smsNotificationSettings.getSmsNotificationAccountId(level) } returns "account-123"
        every { smsNotificationSettings.getSmsNotificationAddress(level) } returns "z1testaddress"
        every { smsNotificationSettings.getSmsNotificationMemo(level) } returns "test memo"
    }

    private fun stubWalletLookup(account: Account, wallet: Wallet) {
        coEvery { accountsStorage.loadAccount("account-123") } returns account
        coEvery { walletManager.getWallets(account) } returns listOf(wallet)
        // For duress mode, active account is different from wallet's account
        every { accountManager.activeAccount } returns createTestAccount(id = "different-account")
    }

    private fun stubWalletLookupForTestTransaction(account: Account, wallet: Wallet) {
        coEvery { accountsStorage.loadAccount(account.id) } returns account
        coEvery { walletManager.getWallets(account) } returns listOf(wallet)
        // For test transaction, wallet is from active account
        every { accountManager.activeAccount } returns account
    }

    private fun stubExistingAdapter(wallet: Wallet, adapter: ISendZcashAdapter) {
        val shieldedToken = createTestToken(TokenType.AddressSpecType.Shielded)
        val unifiedToken = createTestToken(TokenType.AddressSpecType.Unified)

        val shieldedWallet = mockk<Wallet> {
            every { this@mockk.token } returns shieldedToken
            every { this@mockk.account } returns wallet.account
        }
        val unifiedWallet = mockk<Wallet> {
            every { this@mockk.token } returns unifiedToken
            every { this@mockk.account } returns wallet.account
        }

        every { coinManager.getToken(any<TokenQuery>()) } answers {
            val query = firstArg<TokenQuery>()
            if (query.tokenType is TokenType.AddressSpecTyped) {
                when ((query.tokenType as TokenType.AddressSpecTyped).type) {
                    TokenType.AddressSpecType.Shielded -> shieldedToken
                    TokenType.AddressSpecType.Unified -> unifiedToken
                    else -> null
                }
            } else null
        }
        every { walletFactory.create(any(), any(), any()) } answers {
            val token = firstArg<Token>()
            when ((token.type as? TokenType.AddressSpecTyped)?.type) {
                TokenType.AddressSpecType.Shielded -> shieldedWallet
                TokenType.AddressSpecType.Unified -> unifiedWallet
                else -> mockk()
            }
        }

        // Return adapter for shielded, null for unified (simulates only shielded exists)
        // Stub both getAdapterForWallet and awaitAdapterForWallet
        every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(shieldedWallet) } returns adapter
        every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(unifiedWallet) } returns null
        coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(shieldedWallet, any()) } returns adapter
        coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(unifiedWallet, any()) } returns null
    }

    private fun stubSingleExistingAdapter(
        wallet: Wallet,
        adapter: ISendZcashAdapter,
        addressType: TokenType.AddressSpecType
    ) {
        val shieldedToken = createTestToken(TokenType.AddressSpecType.Shielded)
        val unifiedToken = createTestToken(TokenType.AddressSpecType.Unified)

        val shieldedWallet = mockk<Wallet> {
            every { this@mockk.token } returns shieldedToken
            every { this@mockk.account } returns wallet.account
        }
        val unifiedWallet = mockk<Wallet> {
            every { this@mockk.token } returns unifiedToken
            every { this@mockk.account } returns wallet.account
        }

        every { coinManager.getToken(any<TokenQuery>()) } answers {
            val query = firstArg<TokenQuery>()
            if (query.tokenType is TokenType.AddressSpecTyped) {
                when ((query.tokenType as TokenType.AddressSpecTyped).type) {
                    TokenType.AddressSpecType.Shielded -> shieldedToken
                    TokenType.AddressSpecType.Unified -> unifiedToken
                    else -> null
                }
            } else null
        }
        every { walletFactory.create(any(), any(), any()) } answers {
            val token = firstArg<Token>()
            when ((token.type as? TokenType.AddressSpecTyped)?.type) {
                TokenType.AddressSpecType.Shielded -> shieldedWallet
                TokenType.AddressSpecType.Unified -> unifiedWallet
                else -> mockk()
            }
        }

        if (addressType == TokenType.AddressSpecType.Shielded) {
            every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(shieldedWallet) } returns adapter
            every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(unifiedWallet) } returns null
            coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(shieldedWallet, any()) } returns adapter
            coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(unifiedWallet, any()) } returns null
        } else {
            every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(shieldedWallet) } returns null
            every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(unifiedWallet) } returns adapter
            coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(shieldedWallet, any()) } returns null
            coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(unifiedWallet, any()) } returns adapter
        }
    }

    private fun stubBothExistingAdapters(
        wallet: Wallet,
        shieldedAdapter: ISendZcashAdapter,
        unifiedAdapter: ISendZcashAdapter
    ) {
        val shieldedToken = createTestToken(TokenType.AddressSpecType.Shielded)
        val unifiedToken = createTestToken(TokenType.AddressSpecType.Unified)

        val shieldedWallet = mockk<Wallet> {
            every { this@mockk.token } returns shieldedToken
            every { this@mockk.account } returns wallet.account
        }
        val unifiedWallet = mockk<Wallet> {
            every { this@mockk.token } returns unifiedToken
            every { this@mockk.account } returns wallet.account
        }

        every { coinManager.getToken(any<TokenQuery>()) } answers {
            val query = firstArg<TokenQuery>()
            if (query.tokenType is TokenType.AddressSpecTyped) {
                when ((query.tokenType as TokenType.AddressSpecTyped).type) {
                    TokenType.AddressSpecType.Shielded -> shieldedToken
                    TokenType.AddressSpecType.Unified -> unifiedToken
                    else -> null
                }
            } else null
        }
        every { walletFactory.create(any(), any(), any()) } answers {
            val token = firstArg<Token>()
            when ((token.type as? TokenType.AddressSpecTyped)?.type) {
                TokenType.AddressSpecType.Shielded -> shieldedWallet
                TokenType.AddressSpecType.Unified -> unifiedWallet
                else -> mockk()
            }
        }

        every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(shieldedWallet) } returns shieldedAdapter
        every { adapterManager.getAdapterForWallet<ISendZcashAdapter>(unifiedWallet) } returns unifiedAdapter
        coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(shieldedWallet, any()) } returns shieldedAdapter
        coEvery { adapterManager.awaitAdapterForWallet<ISendZcashAdapter>(unifiedWallet, any()) } returns unifiedAdapter
    }
}
