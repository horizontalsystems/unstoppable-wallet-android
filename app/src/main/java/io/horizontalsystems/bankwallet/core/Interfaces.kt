package io.horizontalsystems.bankwallet.core

import android.text.SpannableString
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.factories.PriceAlertItem
import io.horizontalsystems.bankwallet.core.managers.RateDirectionMap
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.core.entities.AppVersion
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Unit>
    fun preloadAdapters()
    fun refresh()
    fun stopKits()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getTransactionsAdapterForWallet(wallet: Wallet): ITransactionsAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
}

interface ILocalStorage {
    var isBackedUp: Boolean
    var sendInputType: SendModule.InputType?
    var iUnderstand: Boolean
    var baseCurrencyCode: String?
    var blockTillDate: Long?

    var baseBitcoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var baseBinanceProvider: String?
    var baseEosProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var isLockTimeEnabled: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: AccountType.Derivation?
    var torEnabled: Boolean

    fun clear()
}

interface IChartTypeStorage {
    var chartType: ChartType?
}

interface IAccountManager {
    val isAccountsEmpty: Boolean
    val accounts: List<Account>
    val accountsFlowable: Flowable<List<Account>>
    val deleteAccountObservable: Flowable<String>

    fun account(coinType: CoinType): Account?
    fun preloadAccounts()
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

interface IAccountCreator {
    fun newAccount(predefinedAccountType: PredefinedAccountType): Account
    fun restoredAccount(accountType: AccountType): Account
}

interface IAccountFactory {
    fun account(type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account
}

interface IWalletFactory {
    fun wallet(coin: Coin, account: Account, settings: CoinSettings): Wallet
}

interface IWalletStorage {
    fun wallets(accounts: List<Account>): List<Wallet>
    fun enabledCoins(): List<Coin>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun wallet(account: Account, coin: Coin): Wallet?
}

interface IPredefinedAccountTypeManager {
    val allTypes: List<PredefinedAccountType>
    fun account(predefinedAccountType: PredefinedAccountType): Account?
    fun predefinedAccountType(type: AccountType): PredefinedAccountType?
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getTransaction(host: String, path: String): Flowable<JsonObject>
    fun getTransactionWithPost(host: String, path: String, body: Map<String, Any>): Flowable<JsonObject>
    fun ping(host: String, url: String): Flowable<Any>
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
    val hasPrimaryClip: Boolean
}

interface ITransactionDataProviderManager {
    val baseProviderUpdatedSignal: Observable<Unit>

    fun providers(coin: Coin): List<FullTransactionInfoModule.Provider>
    fun baseProvider(coin: Coin): FullTransactionInfoModule.Provider
    fun setBaseProvider(name: String, coin: Coin)

    fun bitcoin(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun dash(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun bitcoinCash(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun ethereum(name: String): FullTransactionInfoModule.EthereumForksProvider
    fun binance(name: String): FullTransactionInfoModule.BinanceProvider
    fun eos(name: String): FullTransactionInfoModule.EosProvider
}

interface IWordsManager {
    var isBackedUp: Boolean
    var backedUpSignal: PublishSubject<Unit>

    fun validate(words: List<String>)
    fun generateWords(count: Int = 12): List<String>
}

sealed class AdapterState {
    object Synced : AdapterState()
    class Syncing(val progress: Int, val lastBlockDate: Date?) : AdapterState()
    object NotSynced : AdapterState()
    object NotReady : AdapterState()
}

interface IEthereumKitManager {
    val ethereumKit: EthereumKit?
    val statusInfo: Map<String, Any>?

    fun ethereumKit(wallet: Wallet): EthereumKit
    fun unlink()
}

interface IEosKitManager {
    val eosKit: EosKit?
    val statusInfo: Map<String, Any>?

    fun eosKit(wallet: Wallet): EosKit
    fun unlink()
}

interface IBinanceKitManager {
    val binanceKit: BinanceChainKit?
    val statusInfo: Map<String, Any>?

    fun binanceKit(wallet: Wallet): BinanceChainKit
    fun unlink()
}

interface ITransactionsAdapter {
    val confirmationsThreshold: Int
    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>>
    val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
}

interface IBalanceAdapter {
    val state: AdapterState
    val stateUpdatedFlowable: Flowable<Unit>

    val balance: BigDecimal
    val balanceLocked: BigDecimal? get() = null
    val balanceUpdatedFlowable: Flowable<Unit>

}

interface IReceiveAdapter {
    val receiveAddress: String
    fun getReceiveAddressType(wallet: Wallet): String?
}

interface ISendBitcoinAdapter {
    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?): Single<Unit>
}

interface ISendDashAdapter {
    fun availableBalance(address: String?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun fee(amount: BigDecimal, address: String?): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String): Single<Unit>
}

interface ISendEthereumAdapter {
    val ethereumBalance: BigDecimal
    val minimumRequiredBalance: BigDecimal
    val minimumSendAmount: BigDecimal

    fun availableBalance(gasPrice: Long, gasLimit: Long?): BigDecimal
    fun fee(gasPrice: Long, gasLimit: Long): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long): Single<Unit>
    fun estimateGasLimit(toAddress: String, value: BigDecimal, gasPrice: Long?): Single<Long>

}

interface ISendBinanceAdapter {
    val availableBalance: BigDecimal
    val availableBinanceBalance: BigDecimal
    val fee: BigDecimal

    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, memo: String?): Single<Unit>
}

interface ISendEosAdapter {
    val availableBalance: BigDecimal

    fun validate(account: String)
    fun send(amount: BigDecimal, account: String, memo: String?): Single<Unit>
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}

interface IAppStatusManager {
    val status: Map<String, Any>
}

interface IAppConfigProvider {
    val companyWebPageLink: String
    val appWebPageLink: String
    val reportEmail: String
    val walletHelpTelegramGroup: String
    val developersTelegramGroup: String
    val ipfsId: String
    val ipfsMainGateway: String
    val ipfsFallbackGateway: String
    val infuraProjectId: String?
    val infuraProjectSecret: String?
    val fiatDecimal: Int
    val maxDecimal: Int
    val currencies: List<Currency>
    val featuredCoins: List<Coin>
    val coins: List<Coin>
}

interface IRateStorage {
    fun latestRateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>
    fun latestRate(coinCode: CoinCode, currencyCode: String): Rate?
    fun rateSingle(coinCode: CoinCode, currencyCode: String, timestamp: Long): Single<Rate>
    fun save(rate: Rate)
    fun saveLatest(rate: Rate)
    fun deleteAll()
}

interface IRateManager {
    fun set(coins: List<String>)
    fun marketInfo(coinCode: String, currencyCode: String): MarketInfo?
    fun getLatestRate(coinCode: String, currencyCode: String): BigDecimal?
    fun marketInfoObservable(coinCode: String, currencyCode: String): Observable<MarketInfo>
    fun marketInfoObservable(currencyCode: String): Observable<Map<String, MarketInfo>>
    fun historicalRate(coinCode: String, currencyCode: String, timestamp: Long): Single<BigDecimal>
    fun chartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo?
    fun chartInfoObservable(coinCode: String, currencyCode: String, chartType: ChartType): Observable<ChartInfo>
    fun refresh()
}

interface IAccountsStorage {
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

interface IPriceAlertsStorage {
    val priceAlertCount: Int
    fun all(): List<PriceAlert>
    fun save(priceAlerts: List<PriceAlert>)
    fun delete(priceAlerts: List<PriceAlert>)
    fun deleteExcluding(coinCodes: List<String>)
}

interface IEmojiHelper {
    val multiAlerts: String

    fun title(signedState: Int): String
    fun body(signedState: Int): String
}

interface IPriceAlertHandler {
    fun handleAlerts(latestRatesMap: Map<String, BigDecimal?>)
}

interface IBackgroundPriceAlertManager {
    fun fetchRates(): Single<Unit>
}

interface INotificationFactory {
    fun notifications(alertItems: List<PriceAlertItem>): List<AlertNotification>
}

interface INotificationManager {
    val isEnabled: Boolean
    fun show(notifications: List<AlertNotification>)
    fun clear()
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun delete(enabledWallets: List<EnabledWallet>)
    fun deleteAll()
}

interface IWalletManager {
    val wallets: List<Wallet>
    val walletsUpdatedObservable: Observable<List<Wallet>>
    fun wallet(coin: Coin): Wallet?

    fun loadWallets()
    fun enable(wallets: List<Wallet>)
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun clear()
}

interface IAppNumberFormatter {
    fun format(coinValue: CoinValue, explicitSign: Boolean = false, realNumber: Boolean = false, trimmable: Boolean = false): String?
    fun format(currencyValue: CurrencyValue, showNegativeSign: Boolean = true, trimmable: Boolean = false, canUseLessSymbol: Boolean = true): String?
    fun formatForRates(currencyValue: CurrencyValue, trimmable: Boolean = false, maxFraction: Int? = null): String?
    fun formatForTransactions(coinValue: CoinValue): String?
    fun formatForTransactions(currencyValue: CurrencyValue, isIncoming: Boolean, canUseLessSymbol: Boolean, trimmable: Boolean): SpannableString
    fun format(value: Double, showSign: Boolean = false, precision: Int = 8): String
    fun format(value: BigDecimal, precision: Int): String?
}

interface IFeeRateProvider {
    fun feeRates(): Single<List<FeeRateInfo>>
}

interface IAddressParser {
    fun parse(paymentAddress: String): AddressData
}

interface IBackgroundRateAlertScheduler {
    fun startPeriodicWorker()
    fun stopPeriodicWorker()
}

interface ICoinSettingsManager {
    var syncMode: SyncMode
    var bitcoinDerivation: AccountType.Derivation
    fun coinSettingsForCreate(coinType: CoinType): CoinSettings
    fun coinSettings(coinType: CoinType): CoinSettings
}

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
    fun clearAccount(coinType: CoinType, accountId: String)
}

interface IRateCoinMapper {
    var convertedCoinMap: MutableMap<String, String>
    var unconvertedCoinMap: MutableMap<String, String>

    fun addCoin(direction: RateDirectionMap, from: String, to: String?)
}

interface IBlockedChartCoins {
    var blockedCoins: MutableList<String>
}

interface INetManager {
    fun start()
    fun stop(): Single<Boolean>
}

enum class FeeRatePriority(val value: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun valueOf(value: Int): FeeRatePriority = values().find { it.value == value } ?: MEDIUM
    }
}
