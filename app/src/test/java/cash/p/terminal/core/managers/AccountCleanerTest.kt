package cash.p.terminal.core.managers

import cash.p.terminal.core.adapters.BitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinCashAdapter
import cash.p.terminal.core.adapters.DashAdapter
import cash.p.terminal.core.adapters.ECashAdapter
import cash.p.terminal.core.adapters.Eip20Adapter
import cash.p.terminal.core.adapters.EvmAdapter
import cash.p.terminal.core.adapters.LitecoinAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.adapters.TronAdapter
import cash.p.terminal.core.storage.MoneroFileDao
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.RemoveMoneroWalletFilesUseCase
import cash.p.terminal.modules.pin.core.PinDbStorage
import io.horizontalsystems.core.ISmsNotificationSettings
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountCleanerTest {

    private lateinit var clearZCashWalletDataUseCase: ClearZCashWalletDataUseCase
    private lateinit var removeMoneroWalletFilesUseCase: RemoveMoneroWalletFilesUseCase
    private lateinit var accountManager: IAccountManager
    private lateinit var adapterManager: IAdapterManager
    private lateinit var walletManager: IWalletManager
    private lateinit var accountCleaner: AccountCleaner
    private lateinit var moneroFileDao: MoneroFileDao
    private lateinit var smsNotificationSettings: ISmsNotificationSettings
    private lateinit var pinDbStorage: PinDbStorage
    private lateinit var locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository

    @Before
    fun setUp() {
        clearZCashWalletDataUseCase = mockk()
        removeMoneroWalletFilesUseCase = mockk()
        accountManager = mockk(relaxed = true)
        adapterManager = mockk(relaxed = true)
        walletManager = mockk(relaxed = true)
        moneroFileDao = mockk(relaxed = true)
        smsNotificationSettings = mockk(relaxed = true)
        pinDbStorage = mockk(relaxed = true)
        locallyCreatedTransactionRepository = mockk(relaxed = true)

        coEvery { clearZCashWalletDataUseCase.invoke(any()) } returns Unit
        coEvery { removeMoneroWalletFilesUseCase.invoke(any<Account>()) } returns true
        coEvery { adapterManager.stopAdapters(any()) } returns Unit
        coEvery { adapterManager.stopAdapters(any(), any()) } returns Unit
        every { walletManager.activeWallets } returns emptyList()
        every { pinDbStorage.getAllLevels() } returns emptyList()

        accountCleaner = AccountCleaner(
            clearZCashWalletDataUseCase,
            removeMoneroWalletFilesUseCase,
            accountManager,
            adapterManager,
            walletManager,
            moneroFileDao,
            smsNotificationSettings,
            pinDbStorage,
            locallyCreatedTransactionRepository,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun clearWalletForAccount_activeZcashWallet_skipsClear() = runTest {
        val account = account("acc-active")
        val token = token(BlockchainType.Zcash)
        val wallet = mockk<cash.p.terminal.wallet.Wallet> {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }

        every { walletManager.activeWallets } returns listOf(wallet)

        accountCleaner.clearWalletForAccount(account.id, token)

        coVerify(exactly = 0) { clearZCashWalletDataUseCase.invoke(any()) }
    }

    @Test
    fun clearWalletForAccount_inactiveZcashWallet_clearsZcash() = runTest {
        val account = account("acc-zcasher")
        val token = token(BlockchainType.Zcash)

        every { walletManager.activeWallets } returns emptyList()

        accountCleaner.clearWalletForAccount(account.id, token)

        coVerify(exactly = 1) { clearZCashWalletDataUseCase.invoke(account.id) }
        coVerify(exactly = 0) { removeMoneroWalletFilesUseCase.invoke(any<Account>()) }
    }

    @Test
    fun clearWalletForAccount_inactiveMoneroWallet_clearsMonero() = runTest {
        val account = account("acc-monero")
        val token = token(BlockchainType.Monero)

        every { accountManager.account(account.id) } returns account
        every { walletManager.activeWallets } returns emptyList()

        accountCleaner.clearWalletForAccount(account.id, token)

        verify(exactly = 1) { accountManager.account(account.id) }
        coVerify(exactly = 1) { removeMoneroWalletFilesUseCase.invoke(account) }
        coVerify(exactly = 1) { moneroFileDao.deleteAssociatedRecord(account.id) }
        coVerify(exactly = 0) { clearZCashWalletDataUseCase.invoke(any()) }
    }

    @Test
    fun clearWalletForAccount_activeMoneroWallet_skipsClear() = runTest {
        val account = account("acc-active-monero")
        val token = token(BlockchainType.Monero)
        val wallet = mockk<cash.p.terminal.wallet.Wallet> {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }

        every { walletManager.activeWallets } returns listOf(wallet)

        accountCleaner.clearWalletForAccount(account.id, token)

        coVerify(exactly = 0) { removeMoneroWalletFilesUseCase.invoke(any<Account>()) }
        verify(exactly = 0) { accountManager.account(any()) }
    }

    @Test
    fun clearWalletForAccount_inactiveLitecoin_clearsMwebData() = runTest {
        val account = account("acc-litecoin")
        val token = token(BlockchainType.Litecoin, TokenType.Mweb)

        mockkObject(LitecoinAdapter)
        every { LitecoinAdapter.clearMweb(any()) } returns Unit
        every { walletManager.activeWallets } returns emptyList()

        accountCleaner.clearWalletForAccount(account.id, token)

        verify(exactly = 1) { LitecoinAdapter.clearMweb(account.id) }
    }

    @Test
    fun clearWalletForAccount_inactiveLitecoin_doesNotClearPublicLitecoinData() = runTest {
        val account = account("acc-litecoin-public")
        val token = token(BlockchainType.Litecoin, TokenType.Mweb)

        mockkObject(LitecoinAdapter)
        every { LitecoinAdapter.clear(any()) } returns Unit
        every { LitecoinAdapter.clearMweb(any()) } returns Unit
        every { walletManager.activeWallets } returns emptyList()

        accountCleaner.clearWalletForAccount(account.id, token)

        verify(exactly = 0) { LitecoinAdapter.clear(any()) }
        verify(exactly = 1) { LitecoinAdapter.clearMweb(account.id) }
    }

    @Test
    fun clearWalletForAccount_activePublicLitecoin_stopsAdaptersAndClearsMwebData() = runTest {
        val account = account("acc-active-litecoin")
        val mwebToken = token(BlockchainType.Litecoin, TokenType.Mweb)
        val token = token(BlockchainType.Litecoin, TokenType.Derived(TokenType.Derivation.Bip84))
        val wallet = mockk<cash.p.terminal.wallet.Wallet> {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }

        mockkObject(LitecoinAdapter)
        every { LitecoinAdapter.clearMweb(any()) } returns Unit
        every { walletManager.activeWallets } returns listOf(wallet)

        accountCleaner.clearWalletForAccount(account.id, mwebToken)

        coVerify(exactly = 1) { adapterManager.stopAdapters(listOf(account.id), BlockchainType.Litecoin) }
        verify(exactly = 1) { LitecoinAdapter.clearMweb(account.id) }
    }

    @Test
    fun clearWalletForAccount_activeMwebLitecoin_stopsAdaptersAndClearsMwebData() = runTest {
        val account = account("acc-active-mweb")
        val token = token(BlockchainType.Litecoin, TokenType.Mweb)
        val wallet = mockk<cash.p.terminal.wallet.Wallet> {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }

        mockkObject(LitecoinAdapter)
        every { LitecoinAdapter.clearMweb(any()) } returns Unit
        every { walletManager.activeWallets } returns listOf(wallet)

        accountCleaner.clearWalletForAccount(account.id, token)

        coVerify(exactly = 1) { adapterManager.stopAdapters(listOf(account.id), BlockchainType.Litecoin) }
        verify(exactly = 1) { LitecoinAdapter.clearMweb(account.id) }
    }

    @Test
    fun clearWalletForAccount_unsupportedBlockchain_ignores() = runTest {
        val account = account("acc-bitcoin")
        val token = token(BlockchainType.Bitcoin)

        every { walletManager.activeWallets } returns emptyList()

        accountCleaner.clearWalletForAccount(account.id, token)

        coVerify(exactly = 0) { clearZCashWalletDataUseCase.invoke(any()) }
        coVerify(exactly = 0) { removeMoneroWalletFilesUseCase.invoke(any<Account>()) }
    }

    @Test
    fun clearAccounts_anyAccount_clearsAllAdapters() = runTest {
        val accountId = "acc-full"
        val account = account(accountId)

        mockAdapterClears()
        every { walletManager.activeWallets } returns emptyList()
        every { accountManager.account(accountId) } returns account

        accountCleaner.clearAccounts(listOf(accountId))

        verifyAdapterClears(accountId)

        coVerify(exactly = 1) { removeMoneroWalletFilesUseCase.invoke(account) }
        coVerify(exactly = 1) { clearZCashWalletDataUseCase.invoke(accountId) }
    }

    @Test
    fun clearAccounts_smsNotificationConfigured_clearsSmsSettings() = runTest {
        val accountId = "acc-sms"

        mockAdapterClears()
        every { pinDbStorage.getAllLevels() } returns listOf(0, 1, 2)
        every { smsNotificationSettings.getSmsNotificationAccountId(0) } returns "other-account"
        every { smsNotificationSettings.getSmsNotificationAccountId(1) } returns accountId
        every { smsNotificationSettings.getSmsNotificationAccountId(2) } returns null

        accountCleaner.clearAccounts(listOf(accountId))

        // Should only clear level 1 where accountId matches
        verify(exactly = 0) { smsNotificationSettings.setSmsNotificationAccountId(0, null) }
        verify(exactly = 1) { smsNotificationSettings.setSmsNotificationAccountId(1, null) }
        verify(exactly = 1) { smsNotificationSettings.setSmsNotificationAddress(1, null) }
        verify(exactly = 1) { smsNotificationSettings.setSmsNotificationMemo(1, null) }
        verify(exactly = 0) { smsNotificationSettings.setSmsNotificationAccountId(2, null) }
    }

    @Test
    fun clearAccounts_noSmsNotification_doesNotClearSettings() = runTest {
        val accountId = "acc-no-sms"

        mockAdapterClears()
        every { pinDbStorage.getAllLevels() } returns listOf(0, 1)
        every { smsNotificationSettings.getSmsNotificationAccountId(any()) } returns "other-account"

        accountCleaner.clearAccounts(listOf(accountId))

        verify(exactly = 0) { smsNotificationSettings.setSmsNotificationAccountId(any(), any()) }
        verify(exactly = 0) { smsNotificationSettings.setSmsNotificationAddress(any(), any()) }
        verify(exactly = 0) { smsNotificationSettings.setSmsNotificationMemo(any(), any()) }
    }

    private fun mockAdapterClears() {
        mockkObject(
            BitcoinAdapter,
            BitcoinCashAdapter,
            ECashAdapter,
            LitecoinAdapter,
            DashAdapter,
            EvmAdapter,
            Eip20Adapter,
            SolanaAdapter,
            TronAdapter
        )

        every { BitcoinAdapter.clear(any()) } returns Unit
        every { BitcoinCashAdapter.clear(any()) } returns Unit
        every { ECashAdapter.clear(any()) } returns Unit
        every { LitecoinAdapter.clear(any()) } returns Unit
        every { LitecoinAdapter.clearMweb(any()) } returns Unit
        every { DashAdapter.clear(any()) } returns Unit
        every { EvmAdapter.clear(any()) } returns Unit
        every { Eip20Adapter.clear(any()) } returns Unit
        every { SolanaAdapter.clear(any()) } returns Unit
        every { TronAdapter.clear(any()) } returns Unit
    }

    private fun verifyAdapterClears(accountId: String) {
        verify(exactly = 1) { BitcoinAdapter.clear(accountId) }
        verify(exactly = 1) { BitcoinCashAdapter.clear(accountId) }
        verify(exactly = 1) { ECashAdapter.clear(accountId) }
        verify(exactly = 1) { LitecoinAdapter.clear(accountId) }
        verify(exactly = 1) { LitecoinAdapter.clearMweb(accountId) }
        verify(exactly = 1) { DashAdapter.clear(accountId) }
        verify(exactly = 1) { EvmAdapter.clear(accountId) }
        verify(exactly = 1) { Eip20Adapter.clear(accountId) }
        verify(exactly = 1) { SolanaAdapter.clear(accountId) }
        verify(exactly = 1) { TronAdapter.clear(accountId) }
    }

    private fun account(id: String) = Account(
        id = id,
        name = "Account-$id",
        type = AccountType.Mnemonic(
            words = List(12) { "word$it" },
            passphrase = ""
        ),
        origin = AccountOrigin.Created,
        level = 0,
        isBackedUp = false,
        isFileBackedUp = false
    )

    private fun token(blockchainType: BlockchainType, tokenType: TokenType = TokenType.Native) = Token(
        coin = Coin(
            uid = "coin-${blockchainType.uid}",
            name = "Coin-${blockchainType.uid}",
            code = blockchainType.uid.uppercase()
        ),
        blockchain = Blockchain(
            type = blockchainType,
            name = blockchainType.uid,
            eip3091url = null
        ),
        type = tokenType,
        decimals = 8
    )
}
