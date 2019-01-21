package io.horizontalsystems.bankwallet.core

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

interface IWalletManager {
    val wallets: List<Wallet>
    val walletsUpdatedSignal: Observable<Unit>

    fun refreshWallets()
    fun initWallets()
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
    var isNewWallet: Boolean
    var failedAttempts: Int?
    var lockoutUptime: Long?
    var baseBitcoinProvider: String?
    var baseEthereumProvider: String?

    fun clearAll()
}

interface ISecuredStorage {
    val authData: AuthData?
    fun saveAuthData(authData: AuthData)
    fun noAuthData(): Boolean
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
    fun getTransaction(host: String, path: String): Flowable<JsonObject>
    fun ping(host: String, url: String): Flowable<JsonObject>
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

    fun providers(coinCode: CoinCode): List<FullTransactionInfoModule.Provider>
    fun baseProvider(coinCode: CoinCode): FullTransactionInfoModule.Provider
    fun setBaseProvider(name: String, coinCode: CoinCode)

    fun bitcoin(name: String): FullTransactionInfoModule.BitcoinForksProvider
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
    fun generateWords(): List<String>
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

    val balanceObservable: Flowable<Double>
    val stateObservable: Flowable<AdapterState>

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

    @Throws(Error.InsufficientAmount::class)
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
    fun clear()
}

interface ILockManager {
    var isLocked: Boolean
    fun lock()
    fun onUnlock()
    fun didEnterBackground()
    fun willEnterForeground()
}

interface IAppConfigProvider {
    val testMode: Boolean
    val localizations: List<String>
    val currencies: List<Currency>
}

interface IOneTimerDelegate {
    fun onFire()
}

interface IRateStorage {
    fun rateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>
    fun save(rate: Rate)
    fun getAll(): Flowable<List<Rate>>
    fun deleteAll()
}

interface ICoinStorage {
    fun coinObservable(coinCode: CoinCode): Flowable<StorableCoin>
    fun save(coin: StorableCoin)
    fun getAll(): Flowable<List<StorableCoin>>
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

sealed class Error : Exception() {
    class InsufficientAmount(val fee: Double) : Error()
}
