package io.horizontalsystems.bankwallet.core

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.modules.settings.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.core.entities.AppVersion
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.Subject
import retrofit2.Response
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>>
    fun preloadAdapters()
    fun refresh()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getAdapterForPlatformCoin(platformCoin: PlatformCoin): IAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}

interface ILocalStorage {
    var amountInputType: AmountInputType?
    var baseCurrencyCode: String?

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
    var checkedTerms: List<Term>
    var mainShowedOnce: Boolean
    var notificationId: String?
    var notificationServerTime: Long
    var currentTheme: ThemeType
    var changelogShownForAppVersion: String?
    var ignoreRootedDeviceWarning: Boolean
    var launchPage: LaunchPage?
    var mainTab: MainModule.MainTab?
    var favoriteCoinIdsMigrated: Boolean
    var fillWalletInfoDone: Boolean
    var marketFavoritesSortingField: SortingField?
    var marketFavoritesMarketField: MarketField?
    var relaunchBySettingChange: Boolean

    fun getSwapProviderId(blockchain: EvmBlockchain): String?
    fun setSwapProviderId(blockchain: EvmBlockchain, providerId: String)

    fun clear()
}

interface IChartTypeStorage {
    var chartInterval: HsTimePeriod
}

interface IRestoreSettingsStorage {
    fun restoreSettings(accountId: String, coinId: String): List<RestoreSettingRecord>
    fun restoreSettings(accountId: String): List<RestoreSettingRecord>
    fun save(restoreSettingRecords: List<RestoreSettingRecord>)
    fun deleteAllRestoreSettings(accountId: String)
}

interface IMarketStorage {
    var currentMarketTab: MarketModule.Tab?
}

interface IAccountManager {
    val activeAccount: Account?
    val activeAccountObservable: Flowable<Optional<Account>>
    val isAccountsEmpty: Boolean
    val accounts: List<Account>
    val accountsFlowable: Flowable<List<Account>>
    val accountsDeletedFlowable: Flowable<Unit>

    fun setActiveAccountId(activeAccountId: String?)
    fun account(id: String): Account?
    fun loadAccounts()
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun clear()
    fun clearAccounts()
}

interface IBackupManager {
    val allBackedUp: Boolean
    val allBackedUpFlowable: Flowable<Boolean>
}

interface IAccountFactory {
    fun account(name: String, type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account
    fun watchAccount(name: String, address: String, domain: String?): Account
    fun getNextWatchAccountName(): String
    fun getNextAccountName(): String
}

interface IWalletStorage {
    fun wallets(account: Account): List<Wallet>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun isEnabled(accountId: String, coinId: String): Boolean
    fun clear()
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int, maxIndex: Int): List<Int>
}

interface INetworkManager {
    fun getMarkdown(host: String, path: String): Single<String>
    fun getReleaseNotes(host: String, path: String): Single<JsonObject>
    fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject>
    fun getTransactionWithPost(
        host: String,
        path: String,
        body: Map<String, Any>
    ): Flowable<JsonObject>

    fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any>
    fun getEvmInfo(host: String, path: String): Single<JsonObject>
    fun getBep2TokeInfo(symbol: String): Single<TokenInfoService.Bep2TokenInfo>
    fun getEvmTokeInfo(apiPath: String, address: String): Single<TokenInfoService.EvmTokenInfo>

    suspend fun subscribe(host: String, path: String, body: String): JsonObject
    suspend fun unsubscribe(host: String, path: String, body: String): JsonObject
    suspend fun getNotifications(host: String, path: String): Response<JsonObject>
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
    val hasPrimaryClip: Boolean
}

interface IWordsManager {
    fun validateChecksum(words: List<String>)
    fun isWordValid(word: String): Boolean
    fun isWordPartiallyValid(word: String): Boolean
    fun generateWords(count: Int = 12): List<String>
}

sealed class AdapterState {
    object Synced : AdapterState()
    data class Syncing(val progress: Int? = null, val lastBlockDate: Date? = null) : AdapterState()
    data class SearchingTxs(val count: Int) : AdapterState()
    data class NotSynced(val error: Throwable) : AdapterState()
}

interface IBinanceKitManager {
    val binanceKit: BinanceChainKit?
    val statusInfo: Map<String, Any>?

    fun binanceKit(wallet: Wallet): BinanceChainKit
    fun unlink()
}

interface ITransactionsAdapter {
    val explorerTitle: String
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    fun getTransactionsAsync(
        from: TransactionRecord?,
        coin: PlatformCoin?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>>

    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlowable(
        coin: PlatformCoin?,
        transactionType: FilterTransactionType
    ): Flowable<List<TransactionRecord>>

    fun getTransactionUrl(transactionHash: String): String?
}

class UnsupportedFilterException : Exception()

interface IBalanceAdapter: IBaseAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlowable: Flowable<Unit>

    val balanceData: BalanceData
    val balanceUpdatedFlowable: Flowable<Unit>
}

data class BalanceData(val available: BigDecimal, val locked: BigDecimal = BigDecimal.ZERO) {
    val total get() = available + locked
}

interface IReceiveAdapter: IBaseAdapter {
    val receiveAddress: String
}

interface ISendBitcoinAdapter {
    val balanceData: BalanceData
    val blockchain: BtcBlockchain
    fun availableBalance(
        feeRate: Long,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal

    fun minimumSendAmount(address: String?): BigDecimal?
    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
    fun fee(
        amount: BigDecimal,
        feeRate: Long,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal?

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(
        amount: BigDecimal,
        address: String,
        feeRate: Long,
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
    fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit>
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}

interface IBaseAdapter {
    val isMainnet: Boolean
}

interface IAccountsStorage {
    var activeAccountId: String?
    val isAccountsEmpty: Boolean

    fun allAccounts(): List<Account>
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun getNonBackedUpCount(): Flowable<Int>
    fun clear()
    fun getDeletedAccountIds(): List<String>
    fun clearDeleted()
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun enabledWallets(accountId: String): List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun delete(enabledWallets: List<EnabledWallet>)
    fun deleteAll()
    fun isEnabled(accountId: String, coinId: String): Boolean
}

interface IWalletManager {
    val activeWallets: List<Wallet>
    val activeWalletsUpdatedObservable: Observable<List<Wallet>>

    fun loadWallets()
    fun save(wallets: List<Wallet>)
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
        code: String,
        coinDecimals: Int,
    ): String

    fun formatCoinShort(
        value: BigDecimal,
        code: String,
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

    fun formatCurrencyValueAsShortened(currencyValue: CurrencyValue): String
    fun formatValueAsDiff(value: Value): String
}

interface IFeeRateProvider {
    val feeRatePriorityList: List<FeeRatePriority>
        get() = listOf()
    fun getFeeRateRange(): ClosedRange<Long>? = null
    suspend fun getFeeRate(feeRatePriority: FeeRatePriority): Long
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
    fun setListener(listener: TorManager.Listener)
    val isTorEnabled: Boolean
    val isTorNotificationEnabled: Boolean
    val torObservable: Subject<TorStatus>
}

interface IRateAppManager {
    val showRateAppObservable: Observable<RateUsType>

    fun onBalancePageActive()
    fun onBalancePageInactive()
    fun onAppLaunch()
    fun forceShow()
}

interface ICoinManager {
    fun getPlatformCoin(coinType: CoinType): PlatformCoin?
}

interface ITermsManager {
    val termsAcceptedSignal: Subject<Boolean>
    val terms: List<Term>
    val termsAccepted: Boolean
    fun update(term: Term)
}

sealed class FeeRatePriority(val titleRes: Int) {
    object LOW : FeeRatePriority(R.string.Send_TxSpeed_Low)
    object RECOMMENDED : FeeRatePriority(R.string.Send_TxSpeed_Recommended)
    object HIGH : FeeRatePriority(R.string.Send_TxSpeed_High)

    class Custom(val value: Long) : FeeRatePriority(R.string.Send_TxSpeed_Custom)
}

interface Clearable {
    fun clear()
}
