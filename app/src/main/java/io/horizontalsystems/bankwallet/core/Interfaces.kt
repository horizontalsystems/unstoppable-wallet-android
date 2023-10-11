package io.horizontalsystems.bankwallet.core

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.Bep2TokenInfoService
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.FeeRates
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.AddressData
import io.horizontalsystems.bankwallet.entities.AppVersion
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.entities.RestoreSettingRecord
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIcon
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockInterval
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.tronkit.transaction.Fee
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import io.horizontalsystems.solanakit.models.Address as SolanaAddress
import io.horizontalsystems.tronkit.models.Address as TronAddress

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>>
    fun refresh()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getAdapterForToken(token: Token): IAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}

interface ILocalStorage {
    var zcashAccountIds: Set<String>
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
    var baseBinanceProvider: String?
    var baseZcashProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var isLockTimeEnabled: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: AccountType.Derivation?
    var torEnabled: Boolean
    var appLaunchCount: Int
    var rateAppLastRequestTime: Long
    var balanceHidden: Boolean
    var balanceAutoHideEnabled: Boolean
    var balanceTotalCoinUid: String?
    var termsAccepted: Boolean
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
    var marketFavoritesSortingField: SortingField?
    var marketFavoritesMarketField: MarketField?
    var relaunchBySettingChange: Boolean
    var marketsTabEnabled: Boolean
    val marketsTabEnabledFlow: StateFlow<Boolean>
    var nonRecommendedAccountAlertDismissedAccounts: Set<String>
    var personalSupportEnabled: Boolean

    fun getSwapProviderId(blockchainType: BlockchainType): String?
    fun setSwapProviderId(blockchainType: BlockchainType, providerId: String)

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
    val accountsFlowable: Flowable<List<Account>>
    val accountsDeletedFlowable: Flowable<Unit>
    val newAccountBackupRequiredFlow: StateFlow<Account?>

    fun setActiveAccountId(activeAccountId: String?)
    fun account(id: String): Account?
    fun save(account: Account)
    fun import(accounts: List<Account>)
    fun update(account: Account)
    fun delete(id: String)
    fun clear()
    fun clearAccounts()
    fun onHandledBackupRequiredNewAccount()
    fun setLevel(level: Int)
    fun updateAccountLevels(accountIds: List<String>, level: Int)
    fun updateMaxLevel(level: Int)
}

interface IBackupManager {
    val allBackedUp: Boolean
    val allBackedUpFlowable: Flowable<Boolean>
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
    fun getNextCexAccountName(cexType: CexType): String
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
    fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject>
    fun getTransactionWithPost(
        host: String,
        path: String,
        body: Map<String, Any>
    ): Flowable<JsonObject>

    fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any>
    fun getEvmInfo(host: String, path: String): Single<JsonObject>
    suspend fun getBep2Tokens(): List<Bep2TokenInfoService.Bep2Token>
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
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
    data class Syncing(val progress: Int? = null, val lastBlockDate: Date? = null) : AdapterState()
    data class SearchingTxs(val count: Int) : AdapterState()
    data class NotSynced(val error: Throwable) : AdapterState()

    override fun toString(): String {
        return when (this) {
            is Synced -> "Synced"
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""} lastBlockDate: $lastBlockDate"
            is SearchingTxs -> "SearchingTxs count: $count"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
        }
    }
}

interface IBinanceKitManager {
    val binanceKit: BinanceChainKit?
    val statusInfo: Map<String, Any>?

    fun binanceKit(wallet: Wallet): BinanceChainKit
    fun unlink(account: Account)
}

interface ITransactionsAdapter {
    val explorerTitle: String
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>>

    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType
    ): Flowable<List<TransactionRecord>>

    fun getTransactionUrl(transactionHash: String): String
}

class UnsupportedFilterException : Exception()

interface IBalanceAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlowable: Flowable<Unit>

    val balanceData: BalanceData
    val balanceUpdatedFlowable: Flowable<Unit>

    fun sendAllowed() = balanceState is AdapterState.Synced
}

data class BalanceData(val available: BigDecimal, val locked: BigDecimal = BigDecimal.ZERO) {
    val total get() = available + locked
}

interface IReceiveAdapter {
    val receiveAddress: String
    val isMainNet: Boolean

    val isAccountActive: Boolean
        get() = true
}

interface ISendBitcoinAdapter {
    val balanceData: BalanceData
    val blockchainType: BlockchainType
    fun availableBalance(
        feeRate: Int,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal

    fun minimumSendAmount(address: String?): BigDecimal?
    fun fee(
        amount: BigDecimal,
        feeRate: Int,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal?

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(
        amount: BigDecimal,
        address: String,
        feeRate: Int,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        logger: AppLogger
    ): Single<Unit>
}

interface ISendEthereumAdapter {
    val evmKitWrapper: EvmKitWrapper
    val balanceData: BalanceData

    fun getTransactionData(amount: BigInteger, address: Address): TransactionData
}

interface ISendBinanceAdapter {
    val availableBalance: BigDecimal
    val availableBinanceBalance: BigDecimal
    val fee: BigDecimal

    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<Unit>
}

interface ISendZcashAdapter {
    val availableBalance: BigDecimal
    val fee: BigDecimal

    suspend fun validate(address: String): ZcashAdapter.ZCashAddressType
    suspend fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Long
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}

interface ISendSolanaAdapter {
    val availableBalance: BigDecimal
    suspend fun send(amount: BigDecimal, to: SolanaAddress): FullTransaction
}

interface ISendTronAdapter {
    val balanceData: BalanceData
    val trxBalanceData: BalanceData

    suspend fun estimateFee(amount: BigInteger, to: TronAddress): List<Fee>
    suspend fun send(amount: BigInteger, to: TronAddress, feeLimit: Long?)
    suspend fun isAddressActive(address: TronAddress): Boolean
    fun isOwnAddress(address: TronAddress): Boolean
}

interface IAccountsStorage {
    var activeAccountId: String?
    val isAccountsEmpty: Boolean

    fun allAccounts(accountsMinLevel: Int): List<Account>
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun getNonBackedUpCount(): Flowable<Int>
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

interface IWalletManager {
    val activeWallets: List<Wallet>
    val activeWalletsUpdatedObservable: Observable<List<Wallet>>

    fun save(wallets: List<Wallet>)
    fun saveEnabledWallets(enabledWallets: List<EnabledWallet>)
    fun delete(wallets: List<Wallet>)
    fun clear()
    fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>)
    fun getWallets(account: Account): List<Wallet>
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

interface IFeeRateProvider {
    val feeRateChangeable: Boolean get() = false
    suspend fun getFeeRates() : FeeRates
}

interface IAddressParser {
    fun parse(paymentAddress: String): AddressData
}

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
}

interface ITorManager {
    fun start()
    fun stop(): Single<Boolean>
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
    val termsAcceptedSignalFlow: Flow<Boolean>
    val terms: List<TermsModule.TermType>
    val allTermsAccepted: Boolean
    fun acceptTerms()
}

interface Clearable {
    fun clear()
}
