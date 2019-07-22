package io.horizontalsystems.bankwallet.core

import android.text.SpannableString
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.managers.ServiceExchangeApi
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

interface IAdapterManager {
    val adapters: List<IAdapter>
    val adaptersUpdatedSignal: Observable<Unit>

    fun refresh()
    fun initAdapters(wallets: List<Wallet>)
    fun stopKits()
}

interface ILocalStorage {
    var currentLanguage: String?
    var isBackedUp: Boolean
    var isBiometricOn: Boolean
    var sendInputType: SendModule.InputType?
    var isLightModeOn: Boolean
    var iUnderstand: Boolean
    var baseCurrencyCode: String?
    var blockTillDate: Long?
    var failedAttempts: Int?
    var lockoutUptime: Long?
    var baseBitcoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var syncMode: SyncMode
    var sortType: BalanceSortType

    fun clear()
}

interface ISecuredStorage {
    val authData: AuthData?
    fun saveAuthData(authData: AuthData)
    fun noAuthData(): Boolean
    val savedPin: String?
    fun savePin(pin: String)
    fun removePin()
    fun pinIsEmpty(): Boolean
}

interface IAccountManager {
    val accounts: List<Account>
    val accountsFlowable: Flowable<List<Account>>
    val deleteAccountObservable: Flowable<String>

    fun account(coinType: CoinType): Account?
    fun preloadAccounts()
    fun create(account: Account)
    fun update(account: Account)
    fun delete(id: String)
}

interface IBackupManager {
    val nonBackedUpCount: Int
    val nonBackedUpCountFlowable: Flowable<Int>
    fun setIsBackedUp(id: String)
}

interface ILaunchManager {
    fun onStart()
}

interface IAccountCreator {
    fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?): Account
    fun createNewAccount(defaultAccountType: DefaultAccountType, enabledDefaults: Boolean = false): Account
}

interface IAccountFactory {
    fun account(type: AccountType, backedUp: Boolean, defaultSyncMode: SyncMode): Account
}

interface IWalletFactory {
    fun wallet(coin: Coin, account: Account, syncMode: SyncMode): Wallet
}

interface IWalletStorage {
    fun wallets(accounts: List<Account>): List<Wallet>
    fun save(wallets: List<Wallet>)
}

interface IPredefinedAccountTypeManager {
    val allTypes: List<IPredefinedAccountType>
    fun account(predefinedAccountType: IPredefinedAccountType): Account?
    fun createAccount(predefinedAccountType: IPredefinedAccountType): Account?
    fun createAllAccounts()
}

interface IPredefinedAccountType {
    val title: String
    val coinCodes: String
    val defaultAccountType: DefaultAccountType
    fun supports(accountType: AccountType): Boolean
}

sealed class DefaultAccountType {
    class Mnemonic(val wordsCount: Int) : DefaultAccountType()
    class Eos : DefaultAccountType()
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getLatestRateData(hostType: ServiceExchangeApi.HostType, currency: String): Single<LatestRateData>
    fun getRateByDay(hostType: ServiceExchangeApi.HostType, coinCode: String, currency: String, timestamp: Long): Single<BigDecimal>
    fun getRateByHour(hostType: ServiceExchangeApi.HostType, coinCode: String, currency: String, timestamp: Long): Single<BigDecimal>
    fun getTransaction(host: String, path: String): Flowable<JsonObject>
    fun ping(host: String, url: String): Flowable<Any>
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): FingerprintManagerCompat.CryptoObject?
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
    val hasPrimaryClip: Boolean
}

interface ICurrencyManager {
    val baseCurrency: Currency
    val baseCurrencyUpdatedSignal: Observable<Unit>
    val currencies: List<Currency>
    fun setBaseCurrency(code: String)
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
}

interface IKeyStoreSafeExecute {
    fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
}

interface IWordsManager {
    var isBackedUp: Boolean
    var backedUpSignal: PublishSubject<Unit>

    fun validate(words: List<String>)
    fun generateWords(count: Int = 12): List<String>
}

interface ILanguageManager {
    var currentLanguage: Locale
    val availableLanguages: List<Locale>
}

sealed class AdapterState {
    object Synced : AdapterState()
    class Syncing(val progress: Int, val lastBlockDate: Date?) : AdapterState()
    object NotSynced : AdapterState()
}

interface IEthereumKitManager {
    val ethereumKit: EthereumKit?

    fun ethereumKit(wallet: Wallet): EthereumKit
    fun unlink()
}

interface IAdapter {
    val wallet: Wallet
    val feeCoinCode: String?

    val decimal: Int
    val confirmationsThreshold: Int

    fun start()
    fun stop()
    fun refresh()

    val lastBlockHeight: Int?
    val lastBlockHeightUpdatedFlowable: Flowable<Unit>

    val state: AdapterState
    val stateUpdatedFlowable: Flowable<Unit>

    val balance: BigDecimal
    val balanceUpdatedFlowable: Flowable<Unit>

    fun getTransactions(from: Pair<String, Int>? = null, limit: Int): Single<List<TransactionRecord>>
    val transactionRecordsFlowable: Flowable<List<TransactionRecord>>

    fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority): Single<Unit>

    fun availableBalance(address: String?, feePriority: FeeRatePriority): BigDecimal
    fun fee(value: BigDecimal, address: String?, feePriority: FeeRatePriority): BigDecimal
    @Throws
    fun validate(address: String)

    fun validate(amount: BigDecimal, address: String?, feePriority: FeeRatePriority): List<SendStateError>
    fun parsePaymentAddress(address: String): PaymentRequestAddress

    val receiveAddress: String

    val debugInfo: String
}

interface ISystemInfoManager {
    var appVersion: String
    var biometryType: BiometryType
    fun phoneHasFingerprintSensor(): Boolean
    fun touchSensorCanBeUsed(): Boolean
}

interface IPinManager {
    val isDeviceLockEnabled: Boolean
    var pin: String?
    val isPinSet: Boolean

    fun safeLoad()
    fun store(pin: String)
    fun validate(pin: String): Boolean
    fun clear()
}

interface ILockManager {
    val lockStateUpdatedSignal: PublishSubject<Unit>
    var isLocked: Boolean
    fun onUnlock()
    fun didEnterBackground()
    fun willEnterForeground()
}

interface IAppConfigProvider {
    val ipfsId: String
    val ipfsMainGateway: String
    val ipfsFallbackGateway: String
    val infuraProjectId: String?
    val infuraProjectSecret: String?
    val fiatDecimal: Int
    val maxDecimal: Int
    val testMode: Boolean
    val localizations: List<String>
    val currencies: List<Currency>
    val defaultCoinCodes: List<String>
    val coins: List<Coin>
    val predefinedAccountTypes: List<IPredefinedAccountType>
}

interface IOneTimerDelegate {
    fun onFire()
}

interface IRateStorage {
    fun latestRateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>
    fun rateSingle(coinCode: CoinCode, currencyCode: String, timestamp: Long): Single<Rate>
    fun save(rate: Rate)
    fun saveLatest(rate: Rate)
    fun deleteAll()
}

interface IAccountsStorage {
    fun allAccounts(): List<Account>
    fun save(account: Account)
    fun delete(id: String)
    fun getNonBackedUpCount(): Flowable<Int>
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun enabledWalletsFlowable(): Flowable<List<EnabledWallet>>
    fun save(coins: List<EnabledWallet>)
    fun deleteAll()
}

interface ILockoutManager {
    fun didFailUnlock()
    fun dropFailedAttempts()

    val currentState: LockoutState
}

interface IUptimeProvider {
    val uptime: Long
}

interface ILockoutUntilDateFactory {
    fun lockoutUntilDate(failedAttempts: Int, lockoutTimestamp: Long, uptime: Long): Date?
}

interface ICurrentDateProvider {
    val currentDate: Date
}

interface IWalletManager {
    val wallets: List<Wallet>
    val walletsObservable: Flowable<List<Wallet>>
    fun wallet(coin: Coin): Wallet?

    fun preloadWallets()
    fun enable(wallets: List<Wallet>)
    fun clear()
}

interface IAppNumberFormatter {
    fun format(coinValue: CoinValue, explicitSign: Boolean = false, realNumber: Boolean = false): String?
    fun formatForTransactions(coinValue: CoinValue): String?
    fun format(currencyValue: CurrencyValue, showNegativeSign: Boolean = true, trimmable: Boolean = false, canUseLessSymbol: Boolean = true): String?
    fun formatForTransactions(currencyValue: CurrencyValue, isIncoming: Boolean): SpannableString
    fun format(value: Double): String
}

interface IFeeRateProvider {
    fun ethereumGasPrice(priority: FeeRatePriority): Long
    fun bitcoinFeeRate(priority: FeeRatePriority): Long
    fun bitcoinCashFeeRate(priority: FeeRatePriority): Long
    fun dashFeeRate(priority: FeeRatePriority): Long
}

sealed class SendStateError {
    object InsufficientAmount : SendStateError()
    object InsufficientFeeBalance : SendStateError()
}

enum class FeeRatePriority(val value: Int) {
    LOWEST(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    HIGHEST(4);

    companion object {
        fun valueOf(value: Int): FeeRatePriority = values().find { it.value == value } ?: MEDIUM
    }
}
