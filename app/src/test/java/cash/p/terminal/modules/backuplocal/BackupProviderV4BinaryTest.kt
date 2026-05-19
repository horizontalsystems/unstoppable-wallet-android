package cash.p.terminal.modules.backuplocal

import android.util.Base64
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.DeniableEncryptionManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.RestoreSettingType
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.storage.BlockchainSettingsStorage
import cash.p.terminal.core.storage.EvmSyncSourceStorage
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.chart.ChartIndicatorSettingsDao
import cash.p.terminal.modules.backuplocal.fullbackup.BackupProvider
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.entities.EnabledWallet
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.solanakit.models.RpcSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for BackupProvider V4 binary backup creation with retry mechanism.
 */
class BackupProviderV4BinaryTest {

    private lateinit var backupProvider: BackupProvider

    // Mocked dependencies
    private val localStorage: ILocalStorage = mockk(relaxed = true)
    private val languageManager: LanguageManager = mockk(relaxed = true)
    private val walletStorage: IEnabledWalletStorage = mockk(relaxed = true)
    private val settingsManager: RestoreSettingsManager = mockk(relaxed = true)
    private val accountManager: IAccountManager = mockk(relaxed = true)
    private val accountFactory: IAccountFactory = mockk(relaxed = true)
    private val walletManager: IWalletManager = mockk(relaxed = true)
    private val restoreSettingsManager: RestoreSettingsManager = mockk(relaxed = true)
    private val blockchainSettingsStorage: BlockchainSettingsStorage = mockk(relaxed = true)
    private val evmBlockchainManager: EvmBlockchainManager = mockk(relaxed = true)
    private val marketFavoritesManager: MarketFavoritesManager = mockk(relaxed = true)
    private val balanceViewTypeManager: BalanceViewTypeManager = mockk(relaxed = true)
    private val appIconService: AppIconService = mockk(relaxed = true)
    private val themeService: ThemeService = mockk(relaxed = true)
    private val chartIndicatorManager: ChartIndicatorManager = mockk(relaxed = true)
    private val chartIndicatorSettingsDao: ChartIndicatorSettingsDao = mockk(relaxed = true)
    private val balanceHiddenManager: BalanceHiddenManager = mockk(relaxed = true)
    private val baseTokenManager: BaseTokenManager = mockk(relaxed = true)
    private val launchScreenService: LaunchScreenService = mockk(relaxed = true)
    private val currencyManager: CurrencyManager = mockk(relaxed = true)
    private val btcBlockchainManager: BtcBlockchainManager = mockk(relaxed = true)
    private val evmSyncSourceManager: EvmSyncSourceManager = mockk(relaxed = true)
    private val evmSyncSourceStorage: EvmSyncSourceStorage = mockk(relaxed = true)
    private val solanaRpcSourceManager: SolanaRpcSourceManager = mockk(relaxed = true)
    private val contactsRepository: ContactsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        // Setup mock responses
        every { accountManager.accounts } returns emptyList()
        every { marketFavoritesManager.getAll() } returns emptyList()
        every { btcBlockchainManager.allBlockchains } returns emptyList()
        every { evmBlockchainManager.allBlockchains } returns emptyList()
        every { contactsRepository.contacts } returns emptyList()
        every { chartIndicatorSettingsDao.getAllBlocking() } returns emptyList()

        // Mock balanceViewTypeManager with proper StateFlow
        every { balanceViewTypeManager.balanceViewType } returns BalanceViewType.CoinThenFiat
        every { balanceViewTypeManager.balanceViewTypeFlow } returns MutableStateFlow(BalanceViewType.CoinThenFiat)

        // Mock currencyManager with proper Currency
        val mockCurrency = Currency("USD", "$", 2, 0)
        every { currencyManager.baseCurrency } returns mockCurrency

        // Mock solanaRpcSourceManager with proper RpcSource
        val mockRpcSource = mockk<RpcSource>(relaxed = true)
        every { mockRpcSource.name } returns "Solana RPC"
        every { solanaRpcSourceManager.rpcSource } returns mockRpcSource

        backupProvider = BackupProvider(
            localStorage = localStorage,
            languageManager = languageManager,
            walletStorage = walletStorage,
            settingsManager = settingsManager,
            accountManager = accountManager,
            accountFactory = accountFactory,
            walletManager = walletManager,
            restoreSettingsManager = restoreSettingsManager,
            blockchainSettingsStorage = blockchainSettingsStorage,
            evmBlockchainManager = evmBlockchainManager,
            marketFavoritesManager = marketFavoritesManager,
            balanceViewTypeManager = balanceViewTypeManager,
            appIconService = appIconService,
            themeService = themeService,
            chartIndicatorManager = chartIndicatorManager,
            chartIndicatorSettingsDao = chartIndicatorSettingsDao,
            balanceHiddenManager = balanceHiddenManager,
            baseTokenManager = baseTokenManager,
            launchScreenService = launchScreenService,
            currencyManager = currencyManager,
            btcBlockchainManager = btcBlockchainManager,
            evmSyncSourceManager = evmSyncSourceManager,
            evmSyncSourceStorage = evmSyncSourceStorage,
            solanaRpcSourceManager = solanaRpcSourceManager,
            contactsRepository = contactsRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    // region V4 Binary Backup Creation with Retry

    @Test
    fun createFullBackupV4Binary_emptyWalletLists_succeeds() {
        val result = backupProvider.createFullBackupV4Binary(
            accountIds1 = emptyList(),
            passphrase1 = "mainPassword",
            accountIds2 = null,
            passphrase2 = null
        )

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Verify binary format magic bytes "PW4B"
        assertTrue(BackupLocalModule.BackupV4Binary.isBinaryFormat(result))
    }

    @Test
    fun createWalletBackup_litecoinMwebBackup_keepsMetadataAndBirthdayOnMwebEntry() {
        val account = Account(
            id = "account-id",
            name = "Wallet",
            type = AccountType.Mnemonic(List(12) { "abandon" }, ""),
            origin = AccountOrigin.Created,
            level = 0
        )
        val restoreSettings = RestoreSettings().apply {
            this[RestoreSettingType.BirthdayHeight] = "2257920"
        }
        every { settingsManager.settings(account, BlockchainType.Litecoin) } returns restoreSettings
        every { walletStorage.enabledWallets(account.id) } returns listOf(
            EnabledWallet(
                tokenQueryId = "litecoin|derived:bip84",
                accountId = account.id,
                coinName = "Litecoin",
                coinCode = "LTC",
                coinDecimals = 8,
                coinImage = null
            ),
            EnabledWallet(
                tokenQueryId = "litecoin|mweb",
                accountId = account.id,
                coinName = "Litecoin",
                coinCode = "LTC",
                coinDecimals = 8,
                coinImage = null
            )
        )

        val enabledWalletBackups = backupProvider.enabledWalletBackups(account)
        val publicLitecoin = enabledWalletBackups.first {
            it.tokenQueryId == "litecoin|derived:bip84"
        }
        val mwebLitecoin = enabledWalletBackups.first {
            it.tokenQueryId == "litecoin|mweb"
        }

        assertNull(publicLitecoin.settings)
        assertEquals("Litecoin", mwebLitecoin.coinName)
        assertEquals("LTC", mwebLitecoin.coinCode)
        assertEquals(8, mwebLitecoin.decimals)
        assertEquals(
            "2257920",
            mwebLitecoin.settings?.get(RestoreSettingType.BirthdayHeight)
        )
    }

    @Test
    fun createFullBackupV4Binary_dualPasswords_succeedsViaRetryMechanism() {
        // This test verifies the retry mechanism works in BackupProvider
        // Even if passwords derive colliding offsets, retry with new salt should succeed

        val result = backupProvider.createFullBackupV4Binary(
            accountIds1 = emptyList(),
            passphrase1 = "mainPassword123",
            accountIds2 = emptyList(),
            passphrase2 = "duressPassword456"
        )

        assertNotNull(result)
        assertTrue(BackupLocalModule.BackupV4Binary.isBinaryFormat(result))

        // Verify container can be extracted
        val container = BackupLocalModule.BackupV4Binary.extractContainer(result)
        assertNotNull(container)

        // Verify both passwords can decrypt their data
        val data1 = DeniableEncryptionManager.extractMessageFromBytes(container!!, "mainPassword123")
        val data2 = DeniableEncryptionManager.extractMessageFromBytes(container, "duressPassword456")

        assertNotNull("Main password should decrypt data", data1)
        assertNotNull("Duress password should decrypt data", data2)
    }

    @Test
    fun createFullBackupV4Binary_retryMechanism_handlesPotentialCollisions() {
        // Run multiple times to ensure retry mechanism is robust
        repeat(5) { iteration ->
            val result = backupProvider.createFullBackupV4Binary(
                accountIds1 = emptyList(),
                passphrase1 = "password_$iteration",
                accountIds2 = emptyList(),
                passphrase2 = "duress_$iteration"
            )

            assertNotNull("Iteration $iteration should succeed", result)
            assertTrue(BackupLocalModule.BackupV4Binary.isBinaryFormat(result))
        }
    }

    // This test is disabled because it requires ETH-KECCAK-256 algorithm which is only
    // available in Android crypto providers, not in standard JVM unit test environment.
    // The actual wallet encryption with accounts is tested through instrumented tests.
    // @Test
    // fun createFullBackupV4Binary_withAccounts_createsValidBackup() { ... }

    // endregion
}
