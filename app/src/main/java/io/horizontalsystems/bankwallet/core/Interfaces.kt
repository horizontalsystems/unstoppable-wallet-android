package io.horizontalsystems.bankwallet.core

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.settings.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.core.entities.AppVersion
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.*
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
    var launchPage: LaunchPage?
    var mainTab: MainModule.MainTab?
    var customTokensRestoreCompleted: Boolean
    var favoriteCoinIdsMigrated: Boolean
    var marketFavoritesSortingField: SortingField?
    var marketFavoritesMarketField: MarketField?
    var relaunchBySettingChange: Boolean

    fun getSwapProviderId(blockchain: SwapMainModule.Blockchain): String?
    fun setSwapProviderId(blockchain: SwapMainModule.Blockchain, providerId: String)

    fun clear()
}

interface IChartTypeStorage {
    var chartType: ChartType
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
    fun setIsBackedUp(id: String)
}

interface IAccountFactory {
    fun account(type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account
}

interface IWalletStorage {
    fun wallets(account: Account): List<Wallet>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
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
    fun getEvmTokeInfo(tokenType: String, address: String): Single<TokenInfoService.EvmTokenInfo>

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
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    val explorerTitle: String
    fun explorerUrl(transactionHash: String): String?

    fun getTransactionsAsync(from: TransactionRecord?, coin: PlatformCoin?, limit: Int, transactionType: FilterTransactionType): Single<List<TransactionRecord>>
    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlowable(coin: PlatformCoin?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>>
}

class UnsupportedFilterException: Exception()

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
    fun availableBalance(
        feeRate: Long,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal

    fun minimumSendAmount(address: String?): BigDecimal
    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
    fun fee(
        amount: BigDecimal,
        feeRate: Long,
        address: String?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(
        amount: BigDecimal,
        address: String,
        feeRate: Long,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortingType?,
        logger: AppLogger
    ): Single<Unit>
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
    fun send(
        amount: BigDecimal,
        address: String,
        gasPrice: Long,
        gasLimit: Long,
        logger: AppLogger
    ): Single<Unit>

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

    fun validate(address: String): ZcashAdapter.ZCashAddressType
    fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit>
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
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

interface ICustomTokenStorage {
    fun customTokens(platformType: PlatformType, filter: String): List<CustomToken>
    fun customTokens(filter: String): List<CustomToken>
    fun customTokens(coinTypeIds: List<String>): List<CustomToken>
    fun customToken(coinType: CoinType): CustomToken?
    fun save(customTokens: List<CustomToken>)
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

    fun formatCoin(
        value: Number,
        code: String,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int
    ): String

    fun formatFiat(
        value: Number,
        symbol: String,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int
    ): String

    fun getSignificantDecimalFiat(value: BigDecimal): Int
    fun getSignificantDecimalCoin(value: BigDecimal): Int
    fun shortenValue(number: BigDecimal): Pair<BigDecimal, String>
    fun formatCurrencyValueAsShortened(currencyValue: CurrencyValue): String
    fun formatCoinValueAsShortened(number: BigDecimal, code: String): String
    fun formatValueAsDiff(value: Value): String
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

interface ICustomRangedFeeProvider: IFeeRateProvider {
    val customFeeRange: LongRange
}

interface IAddressParser {
    fun parse(paymentAddress: String): AddressData
}

interface IInitialSyncModeSettingsManager {
    fun allSettings(): List<Pair<InitialSyncSetting, Boolean>>
    fun setting(coinType: CoinType, origin: AccountOrigin? = null): InitialSyncSetting?
    fun save(setting: InitialSyncSetting)
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
    fun forceShow()
}

interface ICoinManager {
    fun save(customTokens: List<CustomToken>)
    fun getPlatformCoin(coinType: CoinType): PlatformCoin?
    fun getPlatformCoinsByCoinTypeIds(coinTypeIds: List<String>): List<PlatformCoin>
    fun getPlatformCoins(platformType: PlatformType, filter: String, limit: Int = 20): List<PlatformCoin>
    fun getPlatformCoins(coinTypes: List<CoinType>): List<PlatformCoin>
    fun featuredFullCoins(enabledPlatformCoins: List<PlatformCoin>): List<FullCoin>
    fun fullCoins(filter: String = "", limit: Int = 20): List<FullCoin>
    fun getFullCoin(coinUid: String) : FullCoin?
}

interface IAddTokenBlockchainService {
    fun isValid(reference: String): Boolean
    fun coinType(reference: String): CoinType
    fun customTokenAsync(reference: String): Single<CustomToken>
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

    class Custom(val value: Long, val range: LongRange) : FeeRatePriority()
}

interface Clearable {
    fun clear()
}
