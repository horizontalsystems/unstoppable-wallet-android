package cash.p.terminal.modules.backuplocal.fullbackup

import android.util.Log
import cash.p.terminal.R
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.DeniableEncryptionManager
import cash.p.terminal.core.managers.EncryptDecryptManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.storage.BlockchainSettingsStorage
import cash.p.terminal.core.storage.EvmSyncSourceStorage
import cash.p.terminal.entities.BtcRestoreMode
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import cash.p.terminal.modules.chart.ChartIndicatorSettingsDao
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.settings.appearance.AppIcon
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.settings.appearance.PriceChangeInterval
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.entities.EnabledWallet
import cash.p.terminal.wallet.entities.TokenQuery
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.toRawHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Random
import java.util.UUID

class BackupFileValidator {
    private val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()
    }

    /**
     * Validates JSON-based backup file (V3 and legacy formats).
     * For binary V4 format, use validateBinary() instead.
     */
    fun validate(json: String) {
        // Check for V3 format - no validation needed, will be validated after decryption
        val backupV3 = try {
            gson.fromJson(json, BackupLocalModule.BackupV3::class.java)
        } catch (e: Exception) {
            null
        }
        if (backupV3?.version == BackupLocalModule.BACKUP_VERSION && backupV3.encrypted.isNotEmpty()) {
            return
        }

        // Legacy format validation
        val fullBackup = gson.fromJson(json, FullBackup::class.java)
        val walletBackup = gson.fromJson(json, BackupLocalModule.WalletBackup::class.java)

        @Suppress("SENSELESS_COMPARISON")
        val isSingleWalletBackup =
            fullBackup.settings == null && walletBackup.crypto != null && walletBackup.type != null && walletBackup.version in 1..2
        @Suppress("SENSELESS_COMPARISON")
        val isFullBackup =
            fullBackup.settings != null && fullBackup.version == 2 && walletBackup.crypto == null && walletBackup.type == null

        if (!isSingleWalletBackup && !isFullBackup) {
            throw Exception("Invalid json format")
        }
    }

    /**
     * Validates V4 binary backup file.
     * @param data Binary file contents
     * @throws Exception if format is invalid
     */
    fun validateBinary(data: ByteArray) {
        if (!BackupLocalModule.BackupV4Binary.isBinaryFormat(data)) {
            throw Exception("Invalid binary format: missing magic bytes")
        }

        val version = BackupLocalModule.BackupV4Binary.getVersion(data)
        if (version != BackupLocalModule.BackupV4Binary.VERSION) {
            throw Exception("Unsupported binary version: $version")
        }

        val container = BackupLocalModule.BackupV4Binary.extractContainer(data)
        if (container == null || container.size < 50_000) { // MIN_CONTAINER_SIZE
            throw Exception("Invalid container size")
        }
    }
}


class BackupProvider(
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
    private val walletStorage: IEnabledWalletStorage,
    private val settingsManager: RestoreSettingsManager,
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
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
    private val encryptDecryptManager by lazy { EncryptDecryptManager() }
    private val version = 2
    private val versionV3 = BackupLocalModule.BACKUP_VERSION

    private val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()
    }

    private fun decrypted(crypto: BackupLocalModule.BackupCrypto, passphrase: String): ByteArray {
        val kdfParams = crypto.kdfparams
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams)
            ?: throw RestoreException.EncryptionKeyException
        return decryptedWithKey(crypto, key)
    }

    private fun decryptedWithKey(
        crypto: BackupLocalModule.BackupCrypto,
        key: ByteArray
    ): ByteArray {
        if (EncryptDecryptManager.passwordIsCorrect(crypto.mac, crypto.ciphertext, key)) {
            return encryptDecryptManager.decrypt(crypto.ciphertext, key, crypto.cipherparams.iv)
        } else {
            throw RestoreException.InvalidPasswordException
        }
    }

    /**
     * Derives the encryption key from passphrase using Scrypt KDF.
     * This is expensive (~1 second), so the result should be cached and reused.
     */
    fun deriveKey(
        passphrase: String,
        kdfParams: BackupLocalModule.KdfParams = BackupLocalModule.kdfDefault
    ): ByteArray {
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams)
            ?: throw RestoreException.EncryptionKeyException
        return key
    }

    @Throws
    suspend fun accountType(
        backup: BackupLocalModule.WalletBackup,
        passphrase: String
    ): AccountType? {
        val decrypted = decrypted(backup.crypto, passphrase)
        return BackupLocalModule.getAccountTypeFromData(backup.type, decrypted)
    }

    /**
     * Decrypts wallet backup using a pre-derived key (avoids expensive Scrypt call).
     * Only uses cached key if wallet's kdfparams match; otherwise derives a new key.
     */
    @Throws
    suspend fun accountTypeWithKey(
        backup: BackupLocalModule.WalletBackup,
        cachedKey: ByteArray,
        cachedKdfParams: BackupLocalModule.KdfParams,
        passphrase: String
    ): AccountType? {
        val walletKdfParams = backup.crypto.kdfparams
        val decrypted = if (kdfParamsMatch(cachedKdfParams, walletKdfParams)) {
            decryptedWithKey(backup.crypto, cachedKey)
        } else {
            // KDF params differ, derive key specifically for this wallet
            decrypted(backup.crypto, passphrase)
        }
        return BackupLocalModule.getAccountTypeFromData(backup.type, decrypted)
    }

    /**
     * Checks if two KdfParams are equivalent (same derivation would produce same key).
     */
    private fun kdfParamsMatch(
        a: BackupLocalModule.KdfParams,
        b: BackupLocalModule.KdfParams
    ): Boolean {
        return a.dklen == b.dklen &&
                a.n == b.n &&
                a.p == b.p &&
                a.r == b.r &&
                a.salt == b.salt
    }

    /**
     * Restores single wallet from V4 backup using pre-decrypted WalletBackupItem.
     */
    suspend fun restoreSingleWalletBackup(
        walletBackupItem: WalletBackupItem
    ) {
        restoreWallets(listOf(walletBackupItem))
    }

    suspend fun restoreSingleWalletBackup(
        type: AccountType,
        accountName: String,
        backup: BackupLocalModule.WalletBackup
    ) {
        val account = accountFactory.account(
            accountName,
            type,
            AccountOrigin.Restored,
            backup.manualBackup,
            true
        )
        accountManager.save(account)

        val enabledWalletBackups = backup.enabledWallets ?: listOf()
        val enabledWallets = enabledWalletBackups.map {
            EnabledWallet(
                tokenQueryId = it.tokenQueryId,
                accountId = account.id,
                coinName = it.coinName,
                coinCode = it.coinCode,
                coinDecimals = it.decimals,
                coinImage = null
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
                } else if (type is AccountType.MnemonicMonero) {
                    // For Monero-only accounts without settings in backup, save height from AccountType
                    val restoreSettings = RestoreSettings()
                    restoreSettings.birthdayHeight = type.height
                    restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                }
            }
        }
    }

    private suspend fun restoreWallets(walletBackupItems: List<WalletBackupItem>) {
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
                    coinDecimals = it.decimals,
                    coinImage = null
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
                        restoreSettingsManager.save(
                            restoreSettings,
                            account,
                            tokenQuery.blockchainType
                        )
                    } else if (account.type is AccountType.MnemonicMonero && tokenQuery.blockchainType == BlockchainType.Monero) {
                        // For Monero-only accounts without settings in backup, save height from AccountType
                        val restoreSettings = RestoreSettings()
                        restoreSettings.birthdayHeight =
                            (account.type as AccountType.MnemonicMonero).height
                        restoreSettingsManager.save(
                            restoreSettings,
                            account,
                            tokenQuery.blockchainType
                        )
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

        launchScreenService.setLaunchScreen(settings.launchScreen)
        localStorage.marketsTabEnabled = settings.marketsTabEnabled
        localStorage.balanceTabButtonsEnabled = settings.balanceHideButtons ?: false
        localStorage.priceChangeInterval = settings.priceChangeMode ?: PriceChangeInterval.LAST_24H
        currencyManager.setBaseCurrencyCode(settings.baseCurrency)


        settings.btcModes.forEach { btcMode ->
            val blockchainType = BlockchainType.fromUid(btcMode.blockchainTypeId)

            val restoreMode = BtcRestoreMode.values().firstOrNull { it.raw == btcMode.restoreMode }
            restoreMode?.let { btcBlockchainManager.save(it, blockchainType) }

            val sortMode =
                TransactionDataSortMode.values().firstOrNull { it.raw == btcMode.sortMode }
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
        val rsiDefaults =
            defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.RSI }
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
        val maDefaults =
            defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.MA }
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
        val macdDefaults =
            defaultChartSettings.filter { it.type == ChartIndicatorSetting.IndicatorType.MACD }
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

        fullBackup.settings?.let { restoreSettings(it, passphrase) }

        if (fullBackup.contacts.isNotEmpty()) {
            contactsRepository.restore(fullBackup.contacts)
        }
    }

    suspend fun decryptedFullBackup(
        fullBackup: FullBackup,
        passphrase: String
    ): DecryptedFullBackup {
        return decryptedFullBackupWithKey(fullBackup, null, null, passphrase)
    }

    /**
     * Decrypts full backup using a pre-derived key (avoids expensive Scrypt calls for each wallet).
     * If cachedKey is null, falls back to deriving key from passphrase for each wallet.
     * The cachedKdfParams are used to verify the cached key is valid for each inner wallet's encryption.
     */
    suspend fun decryptedFullBackupWithKey(
        fullBackup: FullBackup,
        cachedKey: ByteArray?,
        cachedKdfParams: BackupLocalModule.KdfParams?,
        passphrase: String
    ): DecryptedFullBackup {
        val walletBackupItems = mutableListOf<WalletBackupItem>()
        val usedNames = mutableSetOf<String>()

        fullBackup.wallets?.forEach { walletBackup2 ->
            val backup = walletBackup2.backup
            val type = if (cachedKey != null && cachedKdfParams != null) {
                accountTypeWithKey(backup, cachedKey, cachedKdfParams, passphrase)
            } else {
                accountType(backup, passphrase)
            }
            if (type == null) { // AccountType not supported for restoration
                return@forEach
            }

            // Skip hardware wallets - they cannot be restored from backup
            // (private keys are stored on hardware device, not in backup)
            if (type is AccountType.HardwareCard) {
                return@forEach
            }

            val name = accountFactory.getUniqueName(walletBackup2.name, usedNames)
            usedNames.add(name)

            val account = if (type.isWatchAccountType) {
                accountFactory.watchAccount(name, type)
            } else {
                accountFactory.account(
                    name,
                    type,
                    AccountOrigin.Restored,
                    backup.manualBackup,
                    true // Restoring from file proves file backup exists
                )
            }

            walletBackupItems.add(
                WalletBackupItem(
                    account = account,
                    enabledWallets = backup.enabledWallets ?: listOf()
                )
            )
        }

        var contacts = listOf<Contact>()
        fullBackup.contacts?.let { contactsCrypto ->
            val decrypted = if (cachedKey != null && cachedKdfParams != null) {
                // Check if cached key can be used for contacts decryption
                if (kdfParamsMatch(cachedKdfParams, contactsCrypto.kdfparams)) {
                    decryptedWithKey(contactsCrypto, cachedKey)
                } else {
                    decrypted(contactsCrypto, passphrase)
                }
            } else {
                decrypted(contactsCrypto, passphrase)
            }
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
        // Filter out hardware wallets - they cannot be backed up (private keys are on hardware)
        val nonWatchAccounts = accounts.filter { !it.isHardwareWalletAccount && !it.isWatchAccount }
            .sortedBy { it.name.lowercase() }
        val watchAccounts = accounts.filter { !it.isHardwareWalletAccount && it.isWatchAccount }
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
            customRpcsCount = decryptedFullBackup.settings?.evmSyncSources?.custom?.ifEmpty { null }?.size
        )

    fun shouldShowReplaceWarning(decryptedFullBackup: DecryptedFullBackup?): Boolean {
        return decryptedFullBackup != null && decryptedFullBackup.contacts.isNotEmpty() && contactsRepository.contacts.isNotEmpty()
    }

    @Throws
    fun createWalletBackup(account: Account, passphrase: String): String {
        // Hardware wallets cannot be backed up - private keys are on hardware device
        if (account.isHardwareWalletAccount) {
            throw IllegalArgumentException("Hardware wallets cannot be backed up")
        }
        val backup = walletBackup(account, passphrase)
        val innerJson = gson.toJson(backup)
        return wrapInV3Format(innerJson, passphrase)
    }

    @Throws
    fun createFullBackup(accountIds: List<String>, passphrase: String): String {
        // Derive key once and reuse for all encryptions
        val cachedKey = deriveBackupKey(passphrase)

        val wallets = accountManager.accounts
            // Exclude hardware wallets - private keys are on hardware, cannot backup
            .filter { !it.isHardwareWalletAccount }
            .filter { it.isWatchAccount || accountIds.contains(it.id) }
            .mapNotNull {
                val accountBackup = walletBackupWithKey(it, cachedKey, passphrase) ?: return@mapNotNull null
                WalletBackup2(it.name, accountBackup)
            }

        val watchlist = marketFavoritesManager.getAll().map { it.coinUid }

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
                val auth = syncSource.auth?.let { encryptedWithKey(it, cachedKey, passphrase) }
                EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), auth)
            }
        }.flatten()

        val evmSyncSources =
            EvmSyncSources(selected = selectedEvmSyncSources, custom = customEvmSyncSources)

        val solanaSyncSource =
            SolanaSyncSource(BlockchainType.Solana.uid, solanaRpcSourceManager.rpcSource.name)

        val chartIndicators = chartIndicators()

        val settings = Settings(
            balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
            appIcon = localStorage.appIcon?.titleText ?: AppIcon.Main.titleText,
            currentTheme = themeService.selectedTheme,
            chartIndicatorsEnabled = localStorage.chartIndicatorsEnabled,
            chartIndicators = chartIndicators,
            balanceAutoHidden = balanceHiddenManager.balanceAutoHidden,
            conversionTokenQueryId = baseTokenManager.token?.tokenQuery?.id,
            language = languageManager.currentLocaleTag,
            launchScreen = launchScreenService.selectedLaunchScreen,
            marketsTabEnabled = localStorage.marketsTabEnabled,
            balanceHideButtons = localStorage.balanceTabButtonsEnabled,
            baseCurrency = currencyManager.baseCurrency.code,
            btcModes = btcModes,
            priceChangeMode = localStorage.priceChangeInterval,
            evmSyncSources = evmSyncSources,
            solanaSyncSource = solanaSyncSource,
        )

        val contacts = if (contactsRepository.contacts.isNotEmpty())
            encryptedWithKey(contactsRepository.asJsonString, cachedKey, passphrase)
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

        val innerJson = gson.toJson(fullBackup)
        return wrapInV3FormatWithKey(innerJson, cachedKey, passphrase)
    }

    /**
     * Creates V4 deniable encryption backup in binary format.
     * 50% smaller than JSON+Hex format.
     *
     * File structure: [4B magic "PW4B"][1B version][container bytes]
     *
     * Automatically retries with new salt (up to 10 times) if password-derived
     * offsets collide. Each salt produces different offsets, so collisions
     * are resolved by regenerating the salt.
     *
     * @param accountIds1 Primary wallet IDs (can be revealed under duress)
     * @param passphrase1 Primary password
     * @param accountIds2 Hidden wallet IDs (optional, deniable)
     * @param passphrase2 Hidden password (required if accountIds2 provided)
     * @return Binary backup file contents
     * @throws DeniableEncryptionManager.PasswordCollisionException if passwords derive overlapping offsets after all retries
     */
    @Throws(DeniableEncryptionManager.PasswordCollisionException::class)
    fun createFullBackupV4Binary(
        accountIds1: List<String>,
        passphrase1: String,
        accountIds2: List<String>?,
        passphrase2: String?
    ): ByteArray {
        val payload1 = buildV4Payload(
            buildFullBackupJson(accountIds1, passphrase1, includeWatchAccounts = true)
        )

        val payload2 = if (accountIds2 != null && passphrase2 != null) {
            buildV4Payload(
                buildFullBackupJson(accountIds2, passphrase2, includeWatchAccounts = false)
            )
        } else null

        return createDeniableContainerWithRetry(payload1, passphrase1, payload2, passphrase2)
    }

    /**
     * Creates V4 binary backup for a single wallet.
     * Uses the same binary format as full backup for consistency.
     * Single wallet = one password, no deniability.
     *
     * @param account The wallet account to backup
     * @param passphrase Password for encryption
     * @return Binary backup file contents
     */
    fun createSingleWalletBackupV4Binary(
        account: Account,
        passphrase: String
    ): ByteArray {
        if (account.isHardwareWalletAccount) {
            throw IllegalArgumentException("Hardware wallets cannot be backed up")
        }

        val backup = buildSingleWalletFullBackup(account, passphrase) ?: throw IllegalArgumentException("Failed to build wallet backup")
        val payload = buildV4Payload(backup)

        return createDeniableContainerWithRetry(payload, passphrase, null, null)
    }

    /**
     * Serializes FullBackup to padded JSON bytes for V4 container.
     */
    private fun buildV4Payload(fullBackup: FullBackup): ByteArray {
        val json = gson.toJson(fullBackup)
        val padded = addAlignmentPadding(json, TARGET_INNER_JSON_SIZE_V4)
        return padded.toByteArray(Charsets.UTF_8)
    }

    /**
     * Creates deniable container with automatic retry on offset collision.
     */
    private fun createDeniableContainerWithRetry(
        payload1: ByteArray,
        passphrase1: String,
        payload2: ByteArray?,
        passphrase2: String?
    ): ByteArray {
        val containerBytes = DeniableEncryptionManager.createContainerBytes(
            message1 = payload1,
            password1 = passphrase1,
            message2 = payload2,
            password2 = passphrase2
        )
        return BackupLocalModule.BackupV4Binary.create(containerBytes)
    }

    /**
     * Builds FullBackup containing a single wallet.
     * No settings stored - matches V3 behavior (only wallet data).
     */
    private fun buildSingleWalletFullBackup(account: Account, passphrase: String): FullBackup? {
        val cachedKey = deriveBackupKey(passphrase)
        val walletBackup = walletBackupWithKey(account, cachedKey, passphrase) ?: return null

        return FullBackup(
            wallets = listOf(WalletBackup2(account.name, walletBackup)),
            watchlist = null,
            settings = null,
            contacts = null,
            timestamp = System.currentTimeMillis() / 1000,
            version = version,
            id = UUID.randomUUID().toString()
        )
    }

    /**
     * Builds FullBackup object for given account IDs.
     * Used internally by createFullBackupV4Binary to create separate backups for each password.
     */
    private fun buildFullBackupJson(
        accountIds: List<String>,
        passphrase: String,
        includeWatchAccounts: Boolean
    ): FullBackup {
        val cachedKey = deriveBackupKey(passphrase)

        val wallets = accountManager.accounts
            .filter { !it.isHardwareWalletAccount }
            .filter { (includeWatchAccounts && it.isWatchAccount) || accountIds.contains(it.id) }
            .mapNotNull {
                val accountBackup = walletBackupWithKey(it, cachedKey, passphrase) ?: return@mapNotNull null
                WalletBackup2(it.name, accountBackup)
            }

        val watchlist = marketFavoritesManager.getAll().map { it.coinUid }

        val contacts = if (contactsRepository.contacts.isNotEmpty())
            encryptedWithKey(contactsRepository.asJsonString, cachedKey, passphrase)
        else
            null

        return FullBackup(
            wallets = wallets.ifEmpty { null },
            watchlist = watchlist.ifEmpty { null },
            settings = buildCurrentSettings(cachedKey, passphrase),
            contacts = contacts,
            timestamp = System.currentTimeMillis() / 1000,
            version = version,
            id = UUID.randomUUID().toString()
        )
    }

    /**
     * Builds current app settings for backup.
     * Reused by full backup, single wallet backup, and empty backup.
     */
    private fun buildCurrentSettings(cachedKey: ByteArray, passphrase: String): Settings {
        val btcModes = btcBlockchainManager.allBlockchains.map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain.type)
            val sortMode = btcBlockchainManager.transactionSortMode(blockchain.type)
            BtcMode(blockchain.uid, restoreMode.raw, sortMode.raw)
        }

        val selectedEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), null)
        }

        val customEvmSyncSources = evmBlockchainManager.allBlockchains.flatMap { blockchain ->
            evmSyncSourceManager.customSyncSources(blockchain.type).map { syncSource ->
                val auth = syncSource.auth?.let { encryptedWithKey(it, cachedKey, passphrase) }
                EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), auth)
            }
        }

        return Settings(
            balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
            appIcon = localStorage.appIcon?.titleText ?: AppIcon.Main.titleText,
            currentTheme = themeService.selectedTheme,
            chartIndicatorsEnabled = localStorage.chartIndicatorsEnabled,
            chartIndicators = chartIndicators(),
            balanceAutoHidden = balanceHiddenManager.balanceAutoHidden,
            conversionTokenQueryId = baseTokenManager.token?.tokenQuery?.id,
            language = languageManager.currentLocaleTag,
            launchScreen = launchScreenService.selectedLaunchScreen,
            marketsTabEnabled = localStorage.marketsTabEnabled,
            balanceHideButtons = localStorage.balanceTabButtonsEnabled,
            baseCurrency = currencyManager.baseCurrency.code,
            btcModes = btcModes,
            priceChangeMode = localStorage.priceChangeInterval,
            evmSyncSources = EvmSyncSources(selected = selectedEvmSyncSources, custom = customEvmSyncSources),
            solanaSyncSource = SolanaSyncSource(BlockchainType.Solana.uid, solanaRpcSourceManager.rpcSource.name),
        )
    }

    /**
     * Restores from V4 binary backup using provided password.
     * Returns only the data encrypted with this specific password.
     *
     * @param binaryData The binary backup file contents
     * @param passphrase Password to decrypt with
     * @return DecryptedFullBackup if password is correct, null otherwise
     */
    suspend fun restoreFromV4BinaryBackup(
        binaryData: ByteArray,
        passphrase: String
    ): DecryptedFullBackup? {
        // Extract container from binary format
        val container = BackupLocalModule.BackupV4Binary.extractContainer(binaryData)
            ?: return null

        // Extract message from deniable container
        val decryptedBytes =
            DeniableEncryptionManager.extractMessageFromBytes(container, passphrase)
                ?: return null

        val innerJson = String(decryptedBytes, Charsets.UTF_8)

        // Parse as FullBackup
        val fullBackup = try {
            val backup = gson.fromJson(innerJson, FullBackup::class.java)
            backup.version // Verify it's a valid FullBackup
            backup
        } catch (e: Exception) {
            return null
        }

        // Inner wallet decryption uses kdfParams stored in each wallet's crypto field
        return decryptedFullBackup(fullBackup, passphrase)
    }

    /**
     * Checks if binary data is a V4 binary backup.
     */
    fun isV4BinaryBackup(data: ByteArray): Boolean {
        return BackupLocalModule.BackupV4Binary.isBinaryFormat(data)
    }

    private fun wrapInV3Format(innerJson: String, passphrase: String): String {
        return wrapInV3FormatWithKey(innerJson, null, passphrase)
    }

    private fun wrapInV3FormatWithKey(
        innerJson: String,
        cachedKey: ByteArray?,
        passphrase: String
    ): String {
        // Add padding to reach uniform ~100KB file size for privacy
        val alignedInnerJson = addAlignmentPadding(innerJson)

        val crypto = encryptedWithKey(alignedInnerJson, cachedKey, passphrase)
        val cryptoJson = gson.toJson(crypto)
        val base64Encoded = android.util.Base64.encodeToString(
            cryptoJson.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )

        val backupV3 = BackupLocalModule.BackupV3(
            version = versionV3,
            encrypted = base64Encoded
        )
        return gson.toJson(backupV3)
    }

    companion object {
        // Target size for inner JSON before encryption to achieve ~100KB final file
        // Measured expansion ratio: ~1.78x (innerJson → final file)
        // 56KB inner → ~100KB final
        private const val TARGET_INNER_JSON_SIZE = 56_000

        // Target size for V4 inner JSON before encryption
        // V4 container is ~100KB with raw data region
        // Each payload can be up to ~40KB to leave room for two payloads with overhead
        private const val TARGET_INNER_JSON_SIZE_V4 = 40_000

        // Characters safe for JSON string values (alphanumeric)
        private const val PADDING_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    /**
     * Adds random padding to JSON to reach uniform file size (~100KB).
     * This prevents attackers from determining wallet count based on file size.
     * The padding is added as "align_payload" field which is ignored during deserialization.
     */
    private fun addAlignmentPadding(
        json: String,
        targetSize: Int = TARGET_INNER_JSON_SIZE
    ): String {
        val currentSize = json.length

        // If already at or above target, return as-is
        if (currentSize >= targetSize) {
            return json
        }

        // Calculate padding needed
        // Account for wrapper: ,"align_payload":"" (20 chars overhead)
        val wrapperOverhead = 20
        val paddingLength = targetSize - currentSize - wrapperOverhead

        if (paddingLength <= 0) {
            return json
        }

        // Generate cryptographically random padding
        val padding = generateRandomPadding(paddingLength)

        // Insert before closing brace: {...,"align_payload":"xxxxx"}
        val insertPosition = json.lastIndexOf('}')
        if (insertPosition == -1) {
            return json // Invalid JSON, return as-is
        }

        return json.substring(0, insertPosition) +
                ",\"align_payload\":\"$padding\"" +
                json.substring(insertPosition)
    }

    /**
     * Generates a random string of specified length.
     * Uses regular Random (not SecureRandom) since padding is just filler data
     * that will be encrypted anyway - no cryptographic strength needed.
     */
    private fun generateRandomPadding(length: Int): String {
        val random = Random()
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(PADDING_CHARS[random.nextInt(PADDING_CHARS.length)])
        }
        return sb.toString()
    }

    /**
     * Unwraps V3 format and returns the inner JSON along with cached key info.
     * The cached key can be reused for decrypting inner wallet backups if their kdfparams match.
     */
    fun unwrapV3FormatWithKey(
        backupV3: BackupLocalModule.BackupV3,
        passphrase: String
    ): Triple<String, ByteArray, BackupLocalModule.KdfParams> {
        val cryptoJsonBytes =
            android.util.Base64.decode(backupV3.encrypted, android.util.Base64.NO_WRAP)
        val cryptoJson = String(cryptoJsonBytes, Charsets.UTF_8)
        val crypto = gson.fromJson(cryptoJson, BackupLocalModule.BackupCrypto::class.java)

        // Derive key once and return it with kdfparams for reuse
        val kdfParams = crypto.kdfparams
        val key = deriveKey(passphrase, kdfParams)
        val decryptedBytes = decryptedWithKey(crypto, key)
        return Triple(String(decryptedBytes, Charsets.UTF_8), key, kdfParams)
    }

    fun parseV3Backup(json: String): BackupLocalModule.BackupV3? {
        return try {
            val backupV3 = gson.fromJson(json, BackupLocalModule.BackupV3::class.java)
            if (backupV3.version == versionV3 && backupV3.encrypted.isNotEmpty()) backupV3 else null
        } catch (e: Exception) {
            null
        }
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
        return digest.toRawHexString()
    }

    /**
     * Encrypts data using a pre-derived key to avoid repeated Scrypt calls.
     */
    private fun encryptedWithKey(
        data: String,
        cachedKey: ByteArray?,
        passphrase: String
    ): BackupLocalModule.BackupCrypto {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = data.toByteArray(Charsets.UTF_8)

        val key = cachedKey
            ?: (EncryptDecryptManager.getKey(passphrase, kdfParams)
                ?: throw Exception("Couldn't get encryption key"))

        val iv = EncryptDecryptManager.generateRandomBytes(16).toRawHexString()
        val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
        val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

        return BackupLocalModule.BackupCrypto(
            cipher = "aes-128-ctr",
            cipherparams = BackupLocalModule.CipherParams(iv),
            ciphertext = encrypted,
            kdf = "scrypt",
            kdfparams = kdfParams,
            mac = mac.toRawHexString()
        )
    }

    /**
     * Derives encryption key from passphrase. Should be called once and reused.
     */
    private fun deriveBackupKey(passphrase: String): ByteArray {
        return EncryptDecryptManager.getKey(passphrase, BackupLocalModule.kdfDefault)
            ?: throw Exception("Couldn't get encryption key")
    }

    @Throws
    private fun walletBackup(account: Account, passphrase: String): BackupLocalModule.WalletBackup? {
        return walletBackupWithKey(account, null, passphrase)
    }

    /**
     * Creates wallet backup using a pre-derived key to avoid repeated Scrypt calls.
     */
    @Throws
    private fun walletBackupWithKey(
        account: Account,
        cachedKey: ByteArray?,
        passphrase: String
    ): BackupLocalModule.WalletBackup? {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = BackupLocalModule.getDataForEncryption(account.type) ?: return null
        val id = getId(secretText)
        val key = cachedKey ?: (EncryptDecryptManager.getKey(passphrase, kdfParams)
            ?: throw Exception("Couldn't get encryption key"))

        val iv = EncryptDecryptManager.generateRandomBytes(16).toRawHexString()
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
            mac = mac.toRawHexString()
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
    val settings: Settings?,
    val contacts: List<Contact>
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
    val settings: Settings?,
    val contacts: BackupLocalModule.BackupCrypto?,
    val timestamp: Long,
    val version: Int,
    val id: String
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
    val language: String,
    @SerializedName("launch_screen")
    val launchScreen: LaunchPage,
    @SerializedName("show_market")
    val marketsTabEnabled: Boolean,
    @SerializedName("balance_hide_buttons")
    val balanceHideButtons: Boolean?,
    @SerializedName("currency")
    val baseCurrency: String,

    @SerializedName("btc_modes")
    val btcModes: List<BtcMode>,
    @SerializedName("price_change_mode")
    val priceChangeMode: PriceChangeInterval?,
    @SerializedName("evm_sync_sources")
    val evmSyncSources: EvmSyncSources,
    @SerializedName("solana_sync_source")
    val solanaSyncSource: SolanaSyncSource?
)

sealed class RestoreException(message: String) : Exception(message) {
    object EncryptionKeyException : RestoreException("Couldn't get key from passphrase.")
    object InvalidPasswordException :
        RestoreException(cash.p.terminal.strings.helpers.Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword))
}
