package io.horizontalsystems.bankwallet.core

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

interface IWalletManager {
    val wallets: List<Wallet>
    val walletsSubject: PublishSubject<List<Wallet>>

    fun initWallets(words: List<String>, coins: List<Coin>, newWallet: Boolean)
    fun refreshWallets()
    fun clearWallets()
}

interface ILocalStorage {
    var currentLanguage: String?
    var isBackedUp: Boolean
    var isBiometricOn: Boolean
    var isLightModeOn: Boolean
    var iUnderstand: Boolean
    var baseCurrencyCode: String?
    var blockTillDate: Long?
    fun clearAll()
    var isNewWallet: Boolean
    var failedAttempts: Int?
    var lockoutUptime: Long?
}

interface ISecuredStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
    fun wordsAreEmpty(): Boolean
    val savedPin: String?
    fun savePin(pin: String)
    fun pinIsEmpty(): Boolean
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getLatestRate(coin: String, currency: String): Flowable<LatestRate>
    fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double>
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): FingerprintManagerCompat.CryptoObject?
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
}

interface ICurrencyManager {
    val baseCurrency: Currency
    val currencies: List<Currency>
    var subject: PublishSubject<Currency>
    fun setBaseCurrency(code: String)
}

interface IKeyStoreSafeExecute {
    fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
}

interface IWordsManager {
    fun safeLoad()
    var words: List<String>?
    var isBackedUp: Boolean
    var isLoggedIn: Boolean
    var loggedInSubject: PublishSubject<LogInState>
    var backedUpSubject: PublishSubject<Boolean>
    fun createWords()
    fun validate(words: List<String>)
    fun restore(words: List<String>)
    fun logout()
}

enum class LogInState {
    CREATE, RESTORE, RESUME, LOGOUT
}

interface ILanguageManager {
    var currentLanguage: Locale
    var preferredLanguage: Locale?
    val availableLanguages: List<Locale>
}

sealed class AdapterState {
    object Synced : AdapterState()
    class Syncing(val progressSubject: BehaviorSubject<Double>) : AdapterState()
    object NotSynced : AdapterState()
}

interface IAdapter {
    val balance: Double
    val balanceSubject: PublishSubject<Double>

    val state: AdapterState
    val stateSubject: PublishSubject<AdapterState>

    val confirmationsThreshold: Int
    val lastBlockHeight: Int?
    val lastBlockHeightSubject: PublishSubject<Int>

    val transactionRecordsSubject: PublishSubject<List<TransactionRecord>>

    val debugInfo: String

    fun start()
    fun refresh()
    fun clear()

    fun parsePaymentAddress(address: String): PaymentRequestAddress

    fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))? = null)

    @Throws
    fun fee(value: Double, address: String?, senderPay: Boolean): Double

    @Throws
    fun validate(address: String)

    val receiveAddress: String
}

interface ISystemInfoManager {
    var appVersion: String
    var biometryType: BiometryType
    fun phoneHasFingerprintSensor(): Boolean
    fun touchSensorCanBeUsed(): Boolean
}

interface IPinManager {
    fun safeLoad()
    var pin: String?
    val isPinSet: Boolean
    fun store(pin: String)
    fun validate(pin: String): Boolean
}

interface ILockManager {
    var isLocked: Boolean
    fun lock()
    fun onUnlock()
    fun didEnterBackground()
    fun willEnterForeground()
}

interface IAppConfigProvider {
    val network: Network
    val localizations: List<String>
    val currencies: List<Currency>
}

interface IPeriodicTimerDelegate {
    fun onFire()
}

interface IOneTimerDelegate {
    fun onFire()
}

interface IRateSyncerDelegate {
    fun didSync(coin: String, currencyCode: String, latestRate: LatestRate)
}

interface IRateStorage {
    fun rate(coinCode: CoinCode, currencyCode: String): Maybe<Rate>
    fun save(latestRate: LatestRate, coinCode: CoinCode, currencyCode: String)
    fun getAll(): Flowable<List<Rate>>
    fun deleteAll()
}

interface ITransactionRateSyncer {
    fun sync(currencyCode: String)
    fun cancelCurrentSync()
}

interface ITransactionRecordStorage {
    fun record(hash: String): Maybe<TransactionRecord>
    val nonFilledRecords: Maybe<List<TransactionRecord>>
    fun set(rate: Double, transactionHash: String)
    fun clearRates()

    fun update(records: List<TransactionRecord>)
    fun deleteAll()
}

interface ILockoutManager{
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
