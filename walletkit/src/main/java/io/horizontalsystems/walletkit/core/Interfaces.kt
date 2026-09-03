package io.horizontalsystems.walletkit.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.walletkit.core.managers.ActiveAccountState
import io.horizontalsystems.walletkit.core.managers.MiniAppRegisterService.RegisterAppResponse
import io.horizontalsystems.walletkit.core.managers.ServiceWCWhitelist
import io.horizontalsystems.walletkit.core.providers.FeeRates
import io.horizontalsystems.walletkit.core.utils.AddressUriResult
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.AppVersion
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.LaunchPage
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.walletkit.entities.SimulateFailSwapMode
import io.horizontalsystems.walletkit.entities.SyncMode
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.walletkit.modules.amount.AmountInputType
import io.horizontalsystems.walletkit.modules.balance.BalanceSortType
import io.horizontalsystems.walletkit.modules.balance.BalanceViewType
import io.horizontalsystems.walletkit.modules.main.MainModule
import io.horizontalsystems.walletkit.modules.market.MarketModule
import io.horizontalsystems.walletkit.modules.market.TimeDuration
import io.horizontalsystems.walletkit.modules.market.Value
import io.horizontalsystems.walletkit.modules.market.favorites.WatchlistSorting
import io.horizontalsystems.walletkit.modules.roi.PerformanceCoin
import io.horizontalsystems.walletkit.modules.settings.appearance.AppIcon
import io.horizontalsystems.walletkit.modules.settings.appearance.PriceChangeInterval
import io.horizontalsystems.walletkit.modules.settings.privacy.tor.TorStatus
import io.horizontalsystems.walletkit.modules.settings.security.autolock.AutoLockInterval
import io.horizontalsystems.walletkit.modules.settings.terms.TermsModule
import io.horizontalsystems.walletkit.modules.theme.ThemeType
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.util.Date

interface IAdapterManager {
    val adaptersReadyFlow: Flow<Map<Wallet, IAdapter>>
    fun startAdapterManager()
    suspend fun refresh()
    fun <T> getAdapterForWallet(wallet: Wallet): T?
    fun <T> getAdapterForToken(token: Token): T?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshByWallet(wallet: Wallet)
}

interface ILocalStorage {
    var zcashUnshieldedBalanceAlerts: Map<String, BigDecimal>
    var selectedPeriods: List<HsTimePeriod>
    var roiPerformanceCoins: List<PerformanceCoin>
    var marketSearchRecentCoinUids: List<String>
    var swapRecentTokenQueryIds: List<String>
    var uSwapSuspensions: String?
    var uSwapSuspensionsSyncTime: Long
    var privateSendProviderIds: String?
    var privateSendProviderIdsSyncTime: Long
    var zcashAccountIds: Set<String>
    var zcashMigrationTransactionIds: Set<String>
    var autoLockInterval: AutoLockInterval
    var chartIndicatorsEnabled: Boolean
    var amountInputType: AmountInputType?
    var baseCurrencyCode: String?
    var authToken: String?
    val appId: String?

    var baseBitcoinProvider: String?
    var baseLitecoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var baseZcashProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: AccountType.Derivation?
    var torEnabled: Boolean
    var appLaunchCount: Int
    var rateAppLastRequestTime: Long
    var balanceHidden: Boolean
    var balanceAutoHideEnabled: Boolean
    var balanceTotalCoinUid: String?
    var termsAccepted: Boolean
    var swapTermsAccepted: Boolean
    var simulateFailSwap: SimulateFailSwapMode
    var showSwapProviderName: Boolean
    var passkeyTermsAccepted: Boolean
    var checkedTerms: List<String>
    val mainShowedOnceFlow: StateFlow<Boolean>
    var mainShowedOnce: Boolean
    var notificationId: String?
    var notificationServerTime: Long
    var currentTheme: ThemeType
    var balanceViewType: BalanceViewType?
    var changelogShownForAppVersion: String?
    var ignoreRootedDeviceWarning: Boolean
    var launchPage: LaunchPage?
    var appIcon: AppIcon?
    var mainTab: MainModule.MainNavigation?
    fun moneroActiveAccount(accountId: String): Int
    fun setMoneroActiveAccount(accountId: String, accountIndex: Int)
    var marketFavoritesSorting: WatchlistSorting?
    var marketFavoritesShowSignals: Boolean
    var marketFavoritesManualSortingOrder: List<String>
    var marketFavoritesPeriod: TimeDuration?
    var relaunchBySettingChange: Boolean
    var marketsTabEnabled: Boolean
    var recentlySentEnabled: Boolean
    val marketsTabEnabledFlow: StateFlow<Boolean>
    var balanceTabButtonsEnabled: Boolean
    val balanceTabButtonsEnabledFlow: StateFlow<Boolean>
    var amountRoundingEnabled: Boolean
    val amountRoundingEnabledFlow: StateFlow<Boolean>
    var personalSupportEnabled: Boolean
    var hideSuspiciousTransactions: Boolean
    var pinRandomized: Boolean
    var utxoExpertModeEnabled: Boolean
    var rbfEnabled: Boolean
    var statsLastSyncTime: Long
    var uiStatsEnabled: Boolean?
    var recipientAddressCheckEnabled: Boolean
    val utxoExpertModeEnabledFlow: StateFlow<Boolean>
    val marketSignalsStateChangedFlow: SharedFlow<Boolean>

    var priceChangeInterval: PriceChangeInterval
    val priceChangeIntervalFlow: StateFlow<PriceChangeInterval>
    var donateUsLastShownDate: Long?
    var lastMigrationVersion: Int?

    var enabledPaidActions: Set<String>
    val enabledPaidActionsFlow: StateFlow<Set<String>>

    fun migrateEnabledPaidActionsFromDisabled()

    fun clear()
}

interface IRestoreSettingsStorage {
    fun restoreSettings(accountId: String, blockchainTypeUid: String): List<RestoreSettingRecord>
    fun restoreSettings(accountId: String): List<RestoreSettingRecord>
    fun save(restoreSettingRecords: List<RestoreSettingRecord>)
    fun deleteAllRestoreSettings(accountId: String)
}

interface IMarketStorage {
    var currentMarketTab: MarketModule.Tab?
}

interface IAccountManager {
    val hasNonStandardAccount: Boolean
    val activeAccount: Account?
    val activeAccountStateFlow: Flow<ActiveAccountState>
    val isAccountsEmpty: Boolean
    val accounts: List<Account>
    val accountsFlow: Flow<List<Account>>
    val accountsDeletedFlow: Flow<Unit>

    fun setActiveAccountId(activeAccountId: String?)
    fun account(id: String): Account?
    fun save(account: Account)
    fun import(accounts: List<Account>)
    fun update(account: Account)
    fun delete(id: String)
    fun clear()
    fun clearAccounts()
    fun setLevel(level: Int)
    fun updateAccountLevels(accountIds: List<String>, level: Int)
    fun updateMaxLevel(level: Int)
    fun getRandomWalletName(): String
}

interface IBackupManager {
    val allBackedUp: Boolean
    val allBackedUpFlow: Flow<Boolean>
}

interface IAccountFactory {
    fun account(
        name: String,
        type: AccountType,
        origin: AccountOrigin,
        backedUp: Boolean,
        fileBackedUp: Boolean
    ): Account
    fun watchAccount(name: String, type: AccountType): Account
    fun getNextWatchAccountName(): String
    fun getNextAccountName(): String
}

interface IWalletStorage {
    fun wallets(account: Account): List<Wallet>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun handle(newEnabledWallets: List<EnabledWallet>)
    fun clear()
}

interface IRandomProvider {
    fun getRandomNumbers(count: Int, maxIndex: Int): List<Int>
}

interface INetworkManager {
    suspend fun getMarkdown(host: String, path: String): String
    suspend fun getReleaseNotes(host: String, path: String): JsonObject
    suspend fun registerApp(userId: String, referralCode: String): RegisterAppResponse
    suspend fun getWCWhiteList(host: String, path: String): List<ServiceWCWhitelist.WCWhiteList>
}

interface IClipboardManager {
    fun copyText(text: String)

    /**
     * Copies secret material — recovery phrase, private key, passphrase — marking the clip so that
     * supported system and keyboard clients redact it in previews and leave it out of clipboard
     * history. The mark is a hint to those clients, not a boundary: whatever is on the clipboard
     * stays readable by anything that can read the clipboard at all. Use [copyText] for everything
     * else; marking ordinary values sensitive would only hide them from the user.
     */
    fun copySecret(text: String)
    fun getCopiedText(): String?
    val hasPrimaryClip: Boolean
}

interface IWordsManager {
    fun validateChecksum(words: List<String>)
    fun validateChecksumStrict(words: List<String>)
    fun isWordValid(word: String): Boolean
    fun isWordPartiallyValid(word: String): Boolean
    fun generateWords(count: Int = 12): List<String>
}

sealed class AdapterState {
    object Synced : AdapterState()
    object Connecting : AdapterState()
    data class Downloading(val progress: Int? = null) : AdapterState()
    data class Syncing(
        val progress: Int? = null,
        val lastBlockDate: Date? = null,
        val blocksRemained: Long? = null,
    ) : AdapterState()
    data class SearchingTxs(val count: Int) : AdapterState()
    data class NotSynced(val error: Throwable) : AdapterState()

    override fun toString(): String {
        return when (this) {
            is Synced -> "Synced"
            is Connecting -> "Connecting"
            is Downloading -> "Downloading"
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""} lastBlockDate: $lastBlockDate"
            is SearchingTxs -> "SearchingTxs count: $count"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
        }
    }
}

interface ITransactionsAdapter {
    val explorerTitle: String
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlow: Flow<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlow: Flow<Unit>
    val additionalTokenQueries: List<TokenQuery> get() = listOf()

    suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord>

    suspend fun getTransactionsAfter(
        fromTransactionId: String?
    ): List<TransactionRecord> = emptyList()

    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flow<List<TransactionRecord>>

    fun getTransactionUrl(transactionHash: String): String
}

class UnsupportedFilterException : Exception()

interface IBalanceAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlow: Flow<Unit>

    val balanceData: BalanceData?
    val balanceUpdatedFlow: Flow<Unit>
}

data class StellarAssetBalance(val code: String)

data class BalanceData(
    val available: BigDecimal,
    val timeLocked: BigDecimal = BigDecimal.ZERO,
    val notRelayed: BigDecimal = BigDecimal.ZERO,
    val pending: BigDecimal = BigDecimal.ZERO,
    val minimumBalance: BigDecimal = BigDecimal.ZERO,
    val stellarAssets: List<StellarAssetBalance> = listOf(),
    val unshielded: BigDecimal = BigDecimal.ZERO
) {
    val total: BigDecimal
        get() = available + timeLocked + notRelayed + pending + minimumBalance + unshielded

    fun serialize(gson: Gson): String {
        // no need to cache stellarAssets in cache, so we exclude it
        return gson.toJson(this.copy(stellarAssets = listOf()))
    }

    companion object {
        fun deserialize(v: String, gson: Gson): BalanceData? {
            return gson.fromJson(v, BalanceData::class.java)
        }
    }
}

interface IReceiveAdapter {
    val receiveAddress: String
    val isMainNet: Boolean

    val receiveAddressTransparent: String?
        get() = null

    /**
     * Gets a fresh unified/shielded address for receiving funds.
     * For Zcash, this returns a custom unified address with Orchard and Sapling receivers.
     * For other chains, returns the standard receive address.
     */
    suspend fun getFreshReceiveAddress(): String {
        return receiveAddress
    }

    /**
     * Gets a fresh transparent address for receiving funds.
     * For Zcash, this returns a single-use ephemeral transparent address.
     * For other chains, returns the standard transparent address or null.
     */
    suspend fun getFreshReceiveAddressTransparent(): String? {
        return receiveAddressTransparent
    }

    fun usedAddresses(change: Boolean): List<UsedAddress> {
        return listOf()
    }
}

@kotlinx.serialization.Serializable
data class UsedAddress(
    val index: Int,
    val address: String,
    val explorerUrl: String
)




interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}


interface ISendStellarAdapter {
    val maxSendableBalance: BigDecimal
    val fee: BigDecimal
    fun validate(address: String)
    suspend fun getMinimumSendAmount(address: String) : BigDecimal?
    suspend fun send(amount: BigDecimal, address: String, memo: String?)
    suspend fun send(transactionEnvelope: String)
}

interface ISendThorchainAdapter {
    val availableBalance: BigDecimal
    val runeAvailableBalance: BigDecimal
    val fee: BigDecimal
    val isNativeCoin: Boolean
    fun validate(address: String)
    suspend fun send(amount: BigDecimal, address: String, memo: String?): String
}

interface ISendMoneroAdapter {
    val balanceData: BalanceData
    suspend fun getUnspentOutputs(): List<MoneroUnspentOutput>
    suspend fun send(amount: BigDecimal, address: String, memo: String?, selectedOutputs: List<String>? = null): String
    suspend fun estimateFee(amount: BigDecimal, address: String, memo: String?) : BigDecimal
}

data class MoneroUnspentOutput(
    val keyImage: String,
    val amount: BigDecimal,
    val txHash: String,
    val address: String,
    val timestamp: Long?,
)

interface IMoneroAccountsAdapter {
    val accountsFlow: StateFlow<List<MoneroAccountInfo>>
    val activeAccountFlow: StateFlow<Int>
    var activeAccount: Int
    val activeAccountBalanceData: BalanceData
    fun createAccount(label: String?)
    fun renameAccount(accountIndex: Int, label: String)
}

data class MoneroAccountInfo(
    val index: Int,
    val label: String,
    val balance: BigDecimal,
    val unlocked: BigDecimal,
)

interface ISendZanoAdapter {
    val balanceData: BalanceData
    val isNativeAsset: Boolean
    val nativeAvailableBalance: BigDecimal
    suspend fun send(amount: BigDecimal, address: String, memo: String?): String
    suspend fun estimateFee(amount: BigDecimal, address: String, memo: String?) : BigDecimal
}

interface IAccountsStorage {
    val isAccountsEmpty: Boolean

    fun getActiveAccountId(level: Int): String?
    fun setActiveAccountId(level: Int, id: String?)
    fun allAccounts(accountsMinLevel: Int): List<Account>
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun clear()
    fun getDeletedAccountIds(): List<String>
    fun clearDeleted()
    fun updateLevels(accountIds: List<String>, level: Int)
    fun updateMaxLevel(level: Int)
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun enabledWallets(accountId: String): List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun delete(enabledWallets: List<EnabledWallet>)
    fun deleteAll()
}

interface IAppNumberFormatter {
    fun format(
        value: Number,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int,
        prefix: String = "",
        suffix: String = ""
    ): String

    fun formatCoinFull(
        value: BigDecimal,
        code: String?,
        coinDecimals: Int,
    ): String

    fun formatCoinShort(
        value: BigDecimal,
        code: String?,
        coinDecimals: Int
    ): String

    fun formatNumberShort(
        value: BigDecimal,
        maximumFractionDigits: Int
    ): String

    fun formatFiatFull(
        value: BigDecimal,
        symbol: String
    ): String

    fun formatFiatShort(
        value: BigDecimal,
        symbol: String,
        currencyDecimals: Int
    ): String

    fun formatValueAsDiff(value: Value): String
}

interface IAddressParser {
    fun parse(addressUri: String): AddressUriResult
}

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
}

interface ITorManager {
    fun start()
    suspend fun stop(): Boolean
    fun setTorAsEnabled()
    fun setTorAsDisabled()
    val isTorEnabled: Boolean
    val torStatusFlow: StateFlow<TorStatus>
}

interface IRateAppManager {
    val showRateAppFlow: Flow<Boolean>

    fun onBalancePageActive()
    fun onBalancePageInactive()
    fun onAppLaunch()
}

interface ICoinManager {
    fun getToken(query: TokenQuery): Token?
}

interface ITermsManager {
    val termsAcceptedSharedFlow: SharedFlow<Boolean>
    val terms: List<TermsModule.TermType>
    val allTermsAccepted: Boolean
    val checkedTermIds: List<String>
    fun acceptTerms()
    fun broadcastTermsAccepted(accepted: Boolean)
}

interface Clearable {
    fun clear()
}

/** EIP-20 approve/revoke transaction building without exposing ethereum-kit types. */
interface IEip20ApproveAdapter {
    fun buildApproveTransactionData(spender: String, amount: java.math.BigDecimal): io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
    fun buildApproveUnlimitedTransactionData(spender: String): io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
    fun buildRevokeTransactionData(spender: String): io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
}
