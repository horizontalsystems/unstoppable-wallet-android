package cash.p.terminal.modules.backuplocal.fullbackup

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IEnabledWalletStorage
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.EncryptDecryptManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import cash.p.terminal.modules.balance.BalanceViewType
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import java.security.MessageDigest

class BackupProvider(
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
    private val walletStorage: IEnabledWalletStorage,
    private val settingsManager: RestoreSettingsManager,
    private val accountManager: IAccountManager,

    private val evmBlockchainManager: EvmBlockchainManager,

    private val manager: MarketFavoritesManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val chartIndicatorManager: ChartIndicatorManager,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val baseTokenManager: BaseTokenManager,
    private val launchScreenService: LaunchScreenService,
    private val currencyManager: CurrencyManager,

    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,

    private val contactsRepository: ContactsRepository
) {
    private val encryptDecryptManager = EncryptDecryptManager()

    @Throws
    fun backup(passphrase: String): String {
        val wallets = accountManager.accounts.map {
            val accountBackup = accountBackup(it, passphrase)
            WalletBackup2(it.name, accountBackup)
        }

        val watchlist = manager.getAll().map { it.coinUid }

        val swapProviders = evmBlockchainManager.allBlockchainTypes.map { blockchainType ->
            val provider = localStorage.getSwapProviderId(blockchainType) ?: SwapMainModule.OneInchProvider.id
            SwapProvider(blockchainType.uid, provider)
        }

        val btcModes = btcBlockchainManager.allBlockchains.map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain.type)
            val sortMode = btcBlockchainManager.transactionSortMode(blockchain.type)
            BtcMode(blockchain.uid, restoreMode.raw, sortMode.raw)
        }

        val selectedEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            EvmSyncSource(blockchain.uid, syncSource.url.toString(), null)
        }

        val customEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val customEvmSyncSources = evmSyncSourceManager.customSyncSources(blockchain.type)
            customEvmSyncSources.map { syncSource ->
                val auth = syncSource.auth?.let { encrypted(it, passphrase) }
                EvmSyncSource(blockchain.uid, syncSource.url.toString(), auth)
            }
        }.flatten()

        val evmSyncSources = EvmSyncSources(selected = selectedEvmSyncSources, custom = customEvmSyncSources)

        val solanaSyncSource = SolanaSyncSource(BlockchainType.Solana.uid, solanaRpcSourceManager.rpcSource.name)

        val contacts = if (contactsRepository.contacts.isNotEmpty())
            encrypted(contactsRepository.asJsonString, passphrase)
        else
            null

        val settings = Settings(
            balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
            appIcon = appIconService.optionsFlow.value.selected.titleText,
            currentTheme = themeService.optionsFlow.value.selected,
            chartIndicatorsEnabled = chartIndicatorManager.isEnabledFlow.value,
            balanceAutoHidden = balanceHiddenManager.balanceAutoHidden,
            conversionTokenQueryId = baseTokenManager.token?.tokenQuery?.id,
            swapProviders = swapProviders,
            language = languageManager.currentLocaleTag,
            launchScreen = launchScreenService.optionsFlow.value.selected,
            marketsTabEnabled = localStorage.marketsTabEnabled,
            baseCurrency = currencyManager.baseCurrency.code,
            lockTimeEnabled = localStorage.isLockTimeEnabled,
            btcModes = btcModes,
            evmSyncSources = evmSyncSources,
            solanaSyncSource = solanaSyncSource,
        )

        val fullBackup = FullBackup(
            wallets = wallets.ifEmpty { null },
            watchlist = watchlist.ifEmpty { null },
            settings = settings,
            contacts = contacts,
        )

        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()

        return gson.toJson(fullBackup)
    }

    private fun getId(value: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(value)
        return digest.toHexString()
    }

    private fun encrypted(data: String, passphrase: String): BackupLocalModule.BackupCrypto {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = data.toByteArray(Charsets.UTF_8)
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw Exception("Couldn't get encryption key")

        val iv = EncryptDecryptManager.generateRandomBytes(16).toHexString()
        val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
        val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

        return BackupLocalModule.BackupCrypto(
            cipher = "aes-128-ctr",
            cipherparams = BackupLocalModule.CipherParams(iv),
            ciphertext = encrypted,
            kdf = "scrypt",
            kdfparams = kdfParams,
            mac = mac.toHexString()
        )
    }

    @Throws
    fun accountBackup(account: Account, passphrase: String): BackupLocalModule.WalletBackup {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = BackupLocalModule.getDataForEncryption(account.type)
        val id = getId(secretText)
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw Exception("Couldn't get encryption key")

        val iv = EncryptDecryptManager.generateRandomBytes(16).toHexString()
        val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
        val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

        val wallets = walletStorage.enabledWallets(account.id)

        val enabledWalletsBackup = wallets.mapNotNull {
            val tokenQuery = TokenQuery.fromId(it.tokenQueryId) ?: return@mapNotNull null
            val settings = settingsManager.settings(account, tokenQuery.blockchainType).values
            BackupLocalModule.EnabledWalletBackup(
                tokenQueryId = it.tokenQueryId,
                coinName = it.coinName,
                coinCode = it.coinCode,
                decimals = it.coinDecimals,
                settings = settings.ifEmpty { null }
            )
        }

        val crypto = BackupLocalModule.BackupCrypto(
            cipher = "aes-128-ctr",
            cipherparams = BackupLocalModule.CipherParams(iv),
            ciphertext = encrypted,
            kdf = "scrypt",
            kdfparams = kdfParams,
            mac = mac.toHexString()
        )

        return BackupLocalModule.WalletBackup(
            crypto = crypto,
            id = id,
            type = BackupLocalModule.getAccountTypeString(account.type),
            enabledWallets = enabledWalletsBackup,
            manualBackup = account.isBackedUp,
            timestamp = System.currentTimeMillis() / 1000,
            version = 2
        )
    }

}

data class WalletBackup2(
    val name: String,
    val backup: BackupLocalModule.WalletBackup
)

data class FullBackup(
    val wallets: List<WalletBackup2>?,
    val watchlist: List<String>?,
    val settings: Settings?,
    val contacts: BackupLocalModule.BackupCrypto?
)

data class SwapProvider(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    @SerializedName("provider")
    val provider: String
)

data class BtcMode(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    @SerializedName("restore_mode")
    val restoreMode: String,
    @SerializedName("sort_mode")
    val sortMode: String
)

data class EvmSyncSource(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val url: String,
    val auth: BackupLocalModule.BackupCrypto?
)

data class EvmSyncSources(
    val selected: List<EvmSyncSource>,
    val custom: List<EvmSyncSource>
)

data class SolanaSyncSource(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val name: String
)

data class Settings(
    @SerializedName("balance_primary_value")
    val balanceViewType: BalanceViewType,
    @SerializedName("app_icon")
    val appIcon: String,
    @SerializedName("theme_mode")
    val currentTheme: ThemeType,
    @SerializedName("indicators_shown")
    val chartIndicatorsEnabled: Boolean,
    @SerializedName("balance_auto_hide")
    val balanceAutoHidden: Boolean,
    @SerializedName("conversion_token_query_id")
    val conversionTokenQueryId: String?,
    @SerializedName("swap_providers")
    val swapProviders: List<SwapProvider>,
    val language: String,
    @SerializedName("launch_screen")
    val launchScreen: LaunchPage,
    @SerializedName("show_market")
    val marketsTabEnabled: Boolean,
    @SerializedName("currency")
    val baseCurrency: String,
    @SerializedName("lock_time")
    val lockTimeEnabled: Boolean,

    @SerializedName("btc_modes")
    val btcModes: List<BtcMode>,
    @SerializedName("evm_sync_sources")
    val evmSyncSources: EvmSyncSources,
    @SerializedName("solana_sync_source")
    val solanaSyncSource: SolanaSyncSource
)
