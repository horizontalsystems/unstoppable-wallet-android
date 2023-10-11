package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EncryptDecryptManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSettingsDao
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIcon
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIconService
import io.horizontalsystems.bankwallet.modules.settings.appearance.LaunchScreenService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class BackupFileValidator {
    private val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()
    }

    fun validate(json: String) {
        val fullBackup = gson.fromJson(json, FullBackup::class.java)
        val walletBackup = gson.fromJson(json, BackupLocalModule.WalletBackup::class.java)

        if (fullBackup.settings == null && walletBackup.version != 2 && walletBackup.version !in 1..2) {
            throw Exception("Invalid json format")
        }
    }
}


class BackupProvider(
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
    private val walletStorage: IEnabledWalletStorage,
    private val settingsManager: RestoreSettingsManager,
    private val accountManager: IAccountManager,
    private val accountFactory: IAccountFactory,
    private val walletManager: IWalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val chartIndicatorManager: ChartIndicatorManager,
    private val chartIndicatorSettingsDao: ChartIndicatorSettingsDao,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val baseTokenManager: BaseTokenManager,
    private val launchScreenService: LaunchScreenService,
    private val currencyManager: CurrencyManager,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val evmSyncSourceStorage: EvmSyncSourceStorage,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,
    private val contactsRepository: ContactsRepository
) {
    private val encryptDecryptManager = EncryptDecryptManager()
    private val version = 2

    private val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()
    }

    private fun decrypted(crypto: BackupLocalModule.BackupCrypto, passphrase: String): ByteArray {
        val kdfParams = crypto.kdfparams
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw RestoreException.EncryptionKeyException

        if (EncryptDecryptManager.passwordIsCorrect(crypto.mac, crypto.ciphertext, key)) {
            return encryptDecryptManager.decrypt(crypto.ciphertext, key, crypto.cipherparams.iv)
        } else {
            throw RestoreException.InvalidPasswordException
        }
    }

    @Throws
    fun accountType(backup: BackupLocalModule.WalletBackup, passphrase: String): AccountType {
        val decrypted = decrypted(backup.crypto, passphrase)
        return BackupLocalModule.getAccountTypeFromData(backup.type, decrypted)
    }

    fun restoreCexAccount(accountType: AccountType, accountName: String) {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, true, true)
        accountManager.save(account)
    }

    fun restoreSingleWalletBackup(
        type: AccountType,
        accountName: String,
        backup: BackupLocalModule.WalletBackup
    ) {
        val account = accountFactory.account(accountName, type, AccountOrigin.Restored, backup.manualBackup, true)
        accountManager.save(account)

        val enabledWalletBackups = backup.enabledWallets ?: listOf()
        val enabledWallets = enabledWalletBackups.map {
            EnabledWallet(
                tokenQueryId = it.tokenQueryId,
                accountId = account.id,
                coinName = it.coinName,
                coinCode = it.coinCode,
                coinDecimals = it.decimals
            )
        }
        walletManager.saveEnabledWallets(enabledWallets)

        enabledWalletBackups.forEach { enabledWalletBackup ->
            TokenQuery.fromId(enabledWalletBackup.tokenQueryId)?.let { tokenQuery ->
                if (!enabledWalletBackup.settings.isNullOrEmpty()) {
                    val restoreSettings = RestoreSettings()
                    enabledWalletBackup.settings.forEach { (restoreSettingType, value) ->
                        restoreSettings[restoreSettingType] = value
                    }
                    restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                }
            }
        }
    }

    private fun restoreWallets(walletBackupItems: List<WalletBackupItem>) {
        val accounts = mutableListOf<Account>()
        val enabledWallets = mutableListOf<EnabledWallet>()

        walletBackupItems.forEach {
            val account = it.account
            val wallets = it.enabledWallets.map {
                EnabledWallet(
                    tokenQueryId = it.tokenQueryId,
                    accountId = account.id,
                    coinName = it.coinName,
                    coinCode = it.coinCode,
                    coinDecimals = it.decimals
                )
            }

            accounts.add(account)
            enabledWallets.addAll(wallets)

            it.enabledWallets.forEach { enabledWalletBackup ->
                TokenQuery.fromId(enabledWalletBackup.tokenQueryId)?.let { tokenQuery ->
                    if (!enabledWalletBackup.settings.isNullOrEmpty()) {
                        val restoreSettings = RestoreSettings()
                        enabledWalletBackup.settings.forEach { (restoreSettingType, value) ->
                            restoreSettings[restoreSettingType] = value
                        }
                        restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                    }
                }
            }
        }

        if (accounts.isNotEmpty()) {
            accountManager.import(accounts)
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

    private suspend fun restoreSettings(settings: Settings, passphrase: String) {
        balanceViewTypeManager.setViewType(settings.balanceViewType)

        withContext(Dispatchers.Main) {
            try {
                themeService.setThemeType(settings.currentTheme)
                languageManager.currentLocaleTag = settings.language
            } catch (e: Exception) {
                Log.e("e", "theme type restore", e)
            }
        }

        restoreChartSettings(settings.chartIndicatorsEnabled, settings.chartIndicators)

        balanceHiddenManager.setBalanceAutoHidden(settings.balanceAutoHidden)

        settings.conversionTokenQueryId?.let { baseTokenManager.setBaseTokenQueryId(it) }

        settings.swapProviders.forEach {
            localStorage.setSwapProviderId(BlockchainType.fromUid(it.blockchainTypeId), it.provider)
        }

        launchScreenService.setLaunchScreen(settings.launchScreen)
        localStorage.marketsTabEnabled = settings.marketsTabEnabled
        currencyManager.setBaseCurrencyCode(settings.baseCurrency)
        localStorage.isLockTimeEnabled = settings.lockTimeEnabled


        settings.btcModes.forEach { btcMode ->
            val blockchainType = BlockchainType.fromUid(btcMode.blockchainTypeId)

            val restoreMode = BtcRestoreMode.values().firstOrNull { it.raw == btcMode.restoreMode }
            restoreMode?.let { btcBlockchainManager.save(it, blockchainType) }

            val sortMode = TransactionDataSortMode.values().firstOrNull { it.raw == btcMode.sortMode }
            sortMode?.let { btcBlockchainManager.save(sortMode, blockchainType) }
        }

        settings.evmSyncSources.custom.forEach { syncSource ->
            val blockchainType = BlockchainType.fromUid(syncSource.blockchainTypeId)
            val auth = syncSource.auth?.let {
                val decryptedAuth = decrypted(it, passphrase)
                String(decryptedAuth, Charsets.UTF_8)
            }
            evmSyncSourceManager.saveSyncSource(blockchainType, syncSource.url, auth)
        }

        settings.evmSyncSources.selected.forEach { syncSource ->
            val blockchainType = BlockchainType.fromUid(syncSource.blockchainTypeId)
            blockchainSettingsStorage.save(syncSource.url, blockchainType)
        }

        settings.solanaSyncSource?.let {
            blockchainSettingsStorage.save(settings.solanaSyncSource.name, BlockchainType.Solana)
        }

        if (settings.appIcon != (localStorage.appIcon ?: AppIcon.Main).titleText) {
            AppIcon.fromTitle(settings.appIcon)?.let { appIconService.setAppIcon(it) }
        }
    }

    private fun restoreChartSettings(
        chartIndicatorsEnabled: Boolean,
        chartIndicators: ChartIndicators
    ) {
        if (chartIndicatorsEnabled) {
            chartIndicatorManager.enable()
        } else {
            chartIndicatorManager.disable()
        }

        val defaultChartSettings = ChartIndicatorSettingsDao.defaultData()

        val rsi = chartIndicators.rsi
        val rsiDefaults = defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.RSI }
        val rsiChartSettings = rsiDefaults.sortedBy { it.index }.take(rsi.size).map { default ->
            val imported = rsi[default.index - 1]
            default.copy(
                extraData = mapOf(
                    "period" to imported.period.toString()
                ),
                enabled = imported.enabled
            )
        }

        val ma = chartIndicators.ma
        val maDefaults = defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.MA }
        val maChartSettings = maDefaults.sortedBy { it.index }.take(ma.size).map { default ->
            val imported = ma[default.index - 1]
            default.copy(
                extraData = mapOf(
                    "period" to imported.period.toString(),
                    "maType" to imported.type.uppercase(),
                ),
                enabled = imported.enabled
            )
        }

        val macd = chartIndicators.macd
        val macdDefaults = defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.MACD }
        val macdChartSettings = macdDefaults.sortedBy { it.index }.take(macd.size).map { default ->
            val imported = macd[default.index - 1]
            default.copy(
                extraData = mapOf(
                    "fast" to imported.fast.toString(),
                    "slow" to imported.slow.toString(),
                    "signal" to imported.signal.toString(),
                ),
                enabled = imported.enabled
            )
        }

        chartIndicatorSettingsDao.insertAll(rsiChartSettings + maChartSettings + macdChartSettings)
    }

    @Throws
    suspend fun restoreFullBackup(fullBackup: DecryptedFullBackup, passphrase: String) {
        if (fullBackup.wallets.isNotEmpty()) {
            restoreWallets(fullBackup.wallets)
        }

        if (fullBackup.watchlist.isNotEmpty()) {
            marketFavoritesManager.addAll(fullBackup.watchlist)
        }

        restoreSettings(fullBackup.settings, passphrase)

        if (fullBackup.contacts.isNotEmpty()) {
            contactsRepository.restore(fullBackup.contacts)
        }
    }

    fun decryptedFullBackup(fullBackup: FullBackup, passphrase: String): DecryptedFullBackup {
        val walletBackupItems = mutableListOf<WalletBackupItem>()

        fullBackup.wallets?.forEach { walletBackup2 ->
            val backup = walletBackup2.backup
            val type = accountType(backup, passphrase)
            val name = walletBackup2.name

            val account = if (type.isWatchAccountType) {
                accountFactory.watchAccount(name, type)
            } else if (type is AccountType.Cex) {
                accountFactory.account(name, type, AccountOrigin.Restored, true, true)
            } else {
                accountFactory.account(name, type, AccountOrigin.Restored, backup.manualBackup, backup.fileBackup)
            }

            walletBackupItems.add(
                WalletBackupItem(
                    account = account,
                    enabledWallets = backup.enabledWallets ?: listOf()
                )
            )
        }

        var contacts = listOf<Contact>()
        fullBackup.contacts?.let {
            val decrypted = decrypted(it, passphrase)
            val contactsBackupJson = String(decrypted, Charsets.UTF_8)

            contacts = contactsRepository.parseFromJson(contactsBackupJson)
        }

        return DecryptedFullBackup(
            wallets = walletBackupItems,
            watchlist = fullBackup.watchlist ?: listOf(),
            settings = fullBackup.settings,
            contacts = contacts
        )
    }

    private fun fullBackupItems(
        accounts: List<Account>,
        watchlist: List<String>,
        contacts: List<Contact>,
        customRpcsCount: Int?
    ): BackupItems {
        val nonWatchAccounts = accounts.filter { !it.isWatchAccount }.sortedBy { it.name.lowercase() }
        val watchAccounts = accounts.filter { it.isWatchAccount }
        return BackupItems(
            accounts = nonWatchAccounts,
            watchWallets = watchAccounts.ifEmpty { null }?.size,
            watchlist = watchlist.ifEmpty { null }?.size,
            contacts = contacts.ifEmpty { null }?.size,
            customRpc = customRpcsCount,
        )
    }

    fun fullBackupItems() =
        fullBackupItems(
            accounts = accountManager.accounts,
            watchlist = marketFavoritesManager.getAll().map { it.coinUid },
            contacts = contactsRepository.contacts,
            customRpcsCount = evmSyncSourceStorage.getAll().ifEmpty { null }?.size
        )

    fun fullBackupItems(decryptedFullBackup: DecryptedFullBackup) =
        fullBackupItems(
            accounts = decryptedFullBackup.wallets.map { it.account },
            watchlist = decryptedFullBackup.watchlist,
            contacts = decryptedFullBackup.contacts,
            customRpcsCount = decryptedFullBackup.settings.evmSyncSources.custom.ifEmpty { null }?.size
        )

    fun shouldShowReplaceWarning(decryptedFullBackup: DecryptedFullBackup?): Boolean {
        return decryptedFullBackup != null && decryptedFullBackup.contacts.isNotEmpty() && contactsRepository.contacts.isNotEmpty()
    }

    @Throws
    fun createWalletBackup(account: Account, passphrase: String): String {
        val backup = walletBackup(account, passphrase)
        return gson.toJson(backup)
    }

    @Throws
    fun createFullBackup(accountIds: List<String>, passphrase: String): String {
        val wallets = accountManager.accounts
            .filter { it.isWatchAccount || accountIds.contains(it.id) }
            .map {
                val accountBackup = walletBackup(it, passphrase)
                WalletBackup2(it.name, accountBackup)
            }

        val watchlist = marketFavoritesManager.getAll().map { it.coinUid }

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
            EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), null)
        }

        val customEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val customEvmSyncSources = evmSyncSourceManager.customSyncSources(blockchain.type)
            customEvmSyncSources.map { syncSource ->
                val auth = syncSource.auth?.let { encrypted(it, passphrase) }
                EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), auth)
            }
        }.flatten()

        val evmSyncSources = EvmSyncSources(selected = selectedEvmSyncSources, custom = customEvmSyncSources)

        val solanaSyncSource = SolanaSyncSource(BlockchainType.Solana.uid, solanaRpcSourceManager.rpcSource.name)

        val chartIndicators = chartIndicators()

        val settings = Settings(
            balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
            appIcon = localStorage.appIcon?.titleText ?: AppIcon.Main.titleText,
            currentTheme = themeService.optionsFlow.value.selected,
            chartIndicatorsEnabled = localStorage.chartIndicatorsEnabled,
            chartIndicators = chartIndicators,
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

        val contacts = if (contactsRepository.contacts.isNotEmpty())
            encrypted(contactsRepository.asJsonString, passphrase)
        else
            null

        val fullBackup = FullBackup(
            wallets = wallets.ifEmpty { null },
            watchlist = watchlist.ifEmpty { null },
            settings = settings,
            contacts = contacts,
            timestamp = System.currentTimeMillis() / 1000,
            version = version,
            id = UUID.randomUUID().toString()
        )

        return gson.toJson(fullBackup)
    }

    private fun chartIndicators(): ChartIndicators {
        val indicators = chartIndicatorSettingsDao.getAllBlocking()
        val rsi = indicators
            .filter { it.type == ChartIndicatorSetting.IndicatorType.RSI }
            .map { chartIndicatorSetting ->
                val data = chartIndicatorSetting.getTypedDataRsi()
                RsiBackup(
                    period = data.period,
                    enabled = chartIndicatorSetting.enabled
                )
            }
        val ma = indicators
            .filter { it.type == ChartIndicatorSetting.IndicatorType.MA }
            .map { chartIndicatorSetting ->
                val data = chartIndicatorSetting.getTypedDataMA()
                MaBackup(
                    type = data.maType.lowercase(),
                    period = data.period,
                    enabled = chartIndicatorSetting.enabled
                )
            }
        val macd = indicators
            .filter { it.type == ChartIndicatorSetting.IndicatorType.MACD }
            .map { chartIndicatorSetting ->
                val data = chartIndicatorSetting.getTypedDataMacd()
                MacdBackup(
                    fast = data.fast,
                    slow = data.slow,
                    signal = data.signal,
                    enabled = chartIndicatorSetting.enabled
                )
            }
        return ChartIndicators(rsi, ma, macd)
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
    private fun walletBackup(account: Account, passphrase: String): BackupLocalModule.WalletBackup {
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
                tokenQueryId = it.tokenQueryId.lowercase(),
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
            fileBackup = account.isFileBackedUp,
            timestamp = System.currentTimeMillis() / 1000,
            version = version
        )
    }

}

data class WalletBackupItem(
    val account: Account,
    val enabledWallets: List<BackupLocalModule.EnabledWalletBackup>
)

data class DecryptedFullBackup(
    val wallets: List<WalletBackupItem>,
    val watchlist: List<String>,
    val settings: Settings,
    val contacts: List<Contact>
)

data class BackupItem(
    val title: String,
    val subtitle: String
)

data class BackupItems(
    val accounts: List<Account>,
    val watchWallets: Int?,
    val watchlist: Int?,
    val contacts: Int?,
    val customRpc: Int?,
)

data class WalletBackup2(
    val name: String,
    val backup: BackupLocalModule.WalletBackup
)

data class FullBackup(
    val wallets: List<WalletBackup2>?,
    val watchlist: List<String>?,
    val settings: Settings,
    val contacts: BackupLocalModule.BackupCrypto?,
    val timestamp: Long,
    val version: Int,
    val id: String
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

data class EvmSyncSourceBackup(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val url: String,
    val auth: BackupLocalModule.BackupCrypto?
)

data class EvmSyncSources(
    val selected: List<EvmSyncSourceBackup>,
    val custom: List<EvmSyncSourceBackup>
)

data class SolanaSyncSource(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val name: String
)

data class RsiBackup(
    val period: Int,
    val enabled: Boolean
)

data class MaBackup(
    val type: String,
    val period: Int,
    val enabled: Boolean
)

data class MacdBackup(
    val fast: Int,
    val slow: Int,
    val signal: Int,
    val enabled: Boolean
)

data class ChartIndicators(
    val rsi: List<RsiBackup>,
    val ma: List<MaBackup>,
    val macd: List<MacdBackup>
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
    @SerializedName("indicators")
    val chartIndicators: ChartIndicators,
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
    val solanaSyncSource: SolanaSyncSource?
)

sealed class RestoreException(message: String) : Exception(message) {
    object EncryptionKeyException : RestoreException("Couldn't get key from passphrase.")
    object InvalidPasswordException : RestoreException(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword))
}
