package io.horizontalsystems.bankwallet.core

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.core.managers.TorManager
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.settings.theme.ThemeType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.AppVersion
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.Subject
import retrofit2.Response
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Unit>
    fun preloadAdapters()
    fun refresh()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getAdapterForCoin(coin: Coin): IAdapter?
    fun getTransactionsAdapterForWallet(wallet: Wallet): ITransactionsAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}

interface ILocalStorage {
    var sendInputType: SendModule.InputType?
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
    var transactionSortingType: TransactionDataSortingType
    var balanceHidden: Boolean
    var checkedTerms: List<Term>
    var mainShowedOnce: Boolean
    var notificationId: String?
    var notificationServerTime: Long
    var currentTheme: ThemeType
    var changelogShownForAppVersion: String?
    var ignoreRootedDeviceWarning: Boolean

    fun clear()
}

interface IChartTypeStorage {
    var chartType: ChartType?
}

interface IRestoreSettingsStorage {
    fun restoreSettings(accountId: String, coinId: String): List<RestoreSettingRecord>
    fun restoreSettings(accountId: String): List<RestoreSettingRecord>
    fun save(restoreSettingRecords: List<RestoreSettingRecord>)
    fun deleteAllRestoreSettings(accountId: String)
}

interface IMarketStorage {
    var currentTab: MarketModule.Tab?
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
    fun setIsBackedUp(id: String)
}

interface IAccountFactory {
    fun account(type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account
}

interface IWalletStorage {
    fun wallets(accounts: List<Account>): List<Wallet>
    fun wallets(account: Account): List<Wallet>
    fun save(wallets: List<Wallet>)
    fun save(wallet: Wallet)
    fun delete(wallets: List<Wallet>)
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int, maxIndex: Int): List<Int>
}

interface INetworkManager {
    fun getMarkdown(host: String, path: String): Single<String>
    fun getReleaseNotes(host: String, path: String): Single<JsonObject>
    fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject>
    fun getTransactionWithPost(host: String, path: String, body: Map<String, Any>): Flowable<JsonObject>
    fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any>
    fun getEvmInfo(host: String, path: String): Single<JsonObject>

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
    data class Syncing(val progress: Int, val lastBlockDate: Date?) : AdapterState()
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
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>>
    fun getRawTransaction(transactionHash: String): String? = null

    val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
}

interface IBalanceAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlowable: Flowable<Unit>

    val balanceData: BalanceData
    val balanceUpdatedFlowable: Flowable<Unit>
}

data class BalanceData(val available: BigDecimal, val locked: BigDecimal = BigDecimal.ZERO) {
    val total get() = available + locked
}

interface IReceiveAdapter {
    val receiveAddress: String
}

interface ISendBitcoinAdapter {
    val balanceData: BalanceData
    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?, transactionSorting: TransactionDataSortingType?, logger: AppLogger): Single<Unit>
}

interface ISendDashAdapter {
    fun availableBalance(address: String?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun fee(amount: BigDecimal, address: String?): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, logger: AppLogger): Single<Unit>
}

interface ISendEthereumAdapter {
    val evmKit: EthereumKit
    val balanceData: BalanceData
    val ethereumBalance: BigDecimal
    val minimumRequiredBalance: BigDecimal
    val minimumSendAmount: BigDecimal

    fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal
    fun fee(gasPrice: Long, gasLimit: Long): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit>
    fun estimateGasLimit(toAddress: String?, value: BigDecimal, gasPrice: Long?): Single<Long>
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

    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit>
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}

interface IAppConfigProvider {
    val companyWebPageLink: String
    val appWebPageLink: String
    val appGithubLink: String
    val appTwitterLink: String
    val appTelegramLink: String
    val appRedditLink: String
    val reportEmail: String
    val cryptoCompareApiKey: String
    val infuraProjectId: String
    val infuraProjectSecret: String
    val btcCoreRpcUrl: String
    val notificationUrl: String
    val releaseNotesUrl: String
    val etherscanApiKey: String
    val bscscanApiKey: String
    val guidesUrl: String
    val faqUrl: String
    val coinsJsonUrl: String
    val providerCoinsJsonUrl: String
    val fiatDecimal: Int
    val maxDecimal: Int
    val feeRateAdjustForCurrencies: List<String>
    val currencies: List<Currency>
    val featuredCoinTypes: List<CoinType>
}

interface IRateManager {
    fun latestRate(coinType: CoinType, currencyCode: String): LatestRate?
    fun latestRate(coinTypes: List<CoinType>, currencyCode: String): Map<CoinType, LatestRate>
    fun getLatestRate(coinType: CoinType, currencyCode: String): BigDecimal?
    fun latestRateObservable(coinType: CoinType, currencyCode: String): Observable<LatestRate>
    fun latestRateObservable(coinTypes: List<CoinType>, currencyCode: String): Observable<Map<CoinType, LatestRate>>
    fun historicalRateCached(coinType: CoinType, currencyCode: String, timestamp: Long): BigDecimal?
    fun historicalRate(coinType: CoinType, currencyCode: String, timestamp: Long): Single<BigDecimal>
    fun chartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType): ChartInfo?
    fun chartInfoObservable(coinType: CoinType, currencyCode: String, chartType: ChartType): Observable<ChartInfo>
    fun coinMarketDetailsAsync(coinType: CoinType, currencyCode: String, rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>): Single<CoinMarketDetails>
    fun getTopTokenHoldersAsync(coinType: CoinType): Single<List<TokenHolder>>
    fun getTopMarketList(currency: String, itemsCount: Int, diffPeriod: TimePeriod): Single<List<CoinMarket>>
    fun getCoinMarketList(coinTypes: List<CoinType>, currency: String): Single<List<CoinMarket>>
    fun getCoinMarketListByCategory(categoryId: String, currency: String): Single<List<CoinMarket>>
    fun getCoinRatingsAsync(): Single<Map<CoinType, String>>
    fun getGlobalMarketInfoAsync(currency: String): Single<GlobalCoinMarket>
    fun getGlobalCoinMarketPointsAsync(currencyCode: String, timePeriod: TimePeriod): Single<List<GlobalCoinMarketPoint>>
    fun searchCoins(searchText: String): List<CoinData>
    fun getNotificationCoinCode(coinType: CoinType): String?
    fun topDefiTvl(currencyCode: String, fetchDiffPeriod: TimePeriod, itemsCount: Int) : Single<List<DefiTvl>>
    fun defiTvlPoints(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod) : Single<List<DefiTvlPoint>>
    fun getCoinMarketVolumePointsAsync(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod = TimePeriod.HOUR_24): Single<List<CoinMarketPoint>>
    fun getCryptoNews(timestamp: Long? = null): Single<List<CryptoNews>>
    fun refresh(currencyCode: String)
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

interface INotificationManager {
    val enabledInPhone: Boolean
    val enabled: Boolean
    fun clear()
    fun show(notification: AlertNotification)
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun enabledWallets(accountId: String): List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun save(wallet: EnabledWallet)
    fun delete(enabledWallets: List<EnabledWallet>)
    fun deleteAll()
}

interface IBlockchainSettingsStorage {
    var bitcoinCashCoinType: BitcoinCashCoinType?
    fun derivationSetting(coinType: CoinType): DerivationSetting?
    fun saveDerivationSetting(derivationSetting: DerivationSetting)
    fun deleteDerivationSettings()
    fun initialSyncSetting(coinType: CoinType): InitialSyncSetting?
    fun saveInitialSyncSetting(initialSyncSetting: InitialSyncSetting)
    fun ethereumRpcModeSetting(coinType: CoinType): EthereumRpcMode?
    fun saveEthereumRpcModeSetting(ethereumRpcModeSetting: EthereumRpcMode)
}

interface IWalletManager {
    val activeWallets: List<Wallet>
    val activeWalletsUpdatedObservable: Observable<List<Wallet>>

    val wallets: List<Wallet>
    val walletsUpdatedObservable: Observable<List<Wallet>>

    fun loadWallets()
    fun enable(wallets: List<Wallet>)
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun clear()
    fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>)
    fun getWallets(account: Account): List<Wallet>
}

interface IAppNumberFormatter {
    fun format(value: Number, minimumFractionDigits: Int, maximumFractionDigits: Int, prefix: String = "", suffix: String = ""): String
    fun formatCoin(value: Number, code: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String
    fun formatFiat(value: Number, symbol: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String
    fun getSignificantDecimalFiat(value: BigDecimal): Int
    fun getSignificantDecimalCoin(value: BigDecimal): Int
    fun shortenValue(number: Number): Pair<BigDecimal, String>
}

interface IFeeRateProvider {
    val feeRatePriorityList: List<FeeRatePriority>
    val recommendedFeeRate: Single<BigInteger>
    val defaultFeeRatePriority: FeeRatePriority
        get() = FeeRatePriority.RECOMMENDED

    fun feeRate(feeRatePriority: FeeRatePriority): Single<BigInteger> {
        if (feeRatePriority is FeeRatePriority.Custom) {
            return Single.just(feeRatePriority.value.toBigInteger())
        }
        return recommendedFeeRate
    }
}

interface IAddressParser {
    fun parse(paymentAddress: String): AddressData
}

interface IDerivationSettingsManager {
    fun allActiveSettings(): List<Pair<DerivationSetting, CoinType>>
    fun defaultSetting(coinType: CoinType): DerivationSetting?
    fun setting(coinType: CoinType): DerivationSetting?
    fun save(setting: DerivationSetting)
    fun resetStandardSettings()
}

interface IInitialSyncModeSettingsManager {
    fun allSettings(): List<Triple<InitialSyncSetting, Coin, Boolean>>
    fun setting(coinType: CoinType, origin: AccountOrigin? = null): InitialSyncSetting?
    fun save(setting: InitialSyncSetting)
}

interface IEthereumRpcModeSettingsManager {
    val communicationModes: List<CommunicationMode>
    fun rpcMode(): EthereumRpcMode
    fun save(setting: EthereumRpcMode)
}

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
}

interface ITorManager {
    fun start()
    fun stop(): Single<Boolean>
    fun enableTor()
    fun disableTor()
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
    fun onAppBecomeActive()
    fun forceShow()
}

interface ICoinManager {
    val coinAddedObservable: Flowable<List<Coin>>
    val coins: List<Coin>
    val groupedCoins: Pair<List<Coin>, List<Coin>>
    fun getCoin(coinType: CoinType): Coin?
    fun save(coins: List<Coin>)
}

interface IAddTokenBlockchainService {
    fun isValid(reference: String): Boolean
    fun coinType(reference: String): CoinType
    fun coinAsync(reference: String): Single<Coin>
}

interface IPriceAlertManager {
    val notificationChangedFlowable: Flowable<Unit>
    fun getPriceAlerts(): List<PriceAlert>
    fun savePriceAlert(coinType: CoinType, coinName: String, changeState: PriceAlert.ChangeState, trendState: PriceAlert.TrendState)
    fun getAlertStates(coinType: CoinType): Pair<PriceAlert.ChangeState, PriceAlert.TrendState>
    fun hasPriceAlert(coinType: CoinType): Boolean
    fun deactivateAllNotifications()
    fun enablePriceAlerts()
    fun disablePriceAlerts()

    suspend fun fetchNotifications()
}

interface INotificationSubscriptionManager {
    fun addNewJobs(jobs: List<SubscriptionJob>)
    fun processJobs()
}

interface ITermsManager {
    val termsAcceptedSignal: Subject<Boolean>
    val terms: List<Term>
    val termsAccepted: Boolean
    fun update(term: Term)
}

sealed class FeeRatePriority {
    object LOW : FeeRatePriority()
    object RECOMMENDED : FeeRatePriority()
    object HIGH : FeeRatePriority()

    class Custom(val value: Int, val range: IntRange) : FeeRatePriority()
}

interface Clearable {
    fun clear()
}
