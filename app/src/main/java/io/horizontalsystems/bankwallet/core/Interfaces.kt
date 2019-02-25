package io.horizontalsystems.bankwallet.core

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.text.SpannableString
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

interface IAdapterManager {
    val adapters: List<IAdapter>
    val adaptersUpdatedSignal: Observable<Unit>

    fun refreshAdapters()
    fun initAdapters()
    fun clear()
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
    var isNewWallet: Boolean
    var failedAttempts: Int?
    var lockoutUptime: Long?
    var baseBitcoinProvider: String?
    var baseEthereumProvider: String?

    fun clear()
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
    fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<BigDecimal>
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
    val availableLanguages: List<Locale>
}

sealed class AdapterState {
    object Synced : AdapterState()
    class Syncing(val progress: Int, val lastBlockDate: Date?) : AdapterState()
    object NotSynced : AdapterState()
}

interface IEthereumKitManager {
    fun ethereumKit(authData: AuthData): EthereumKit
    fun clear()
    fun unlink()
}

interface IAdapter {
    val coin: Coin
    val feeCoinCode: String?
    val decimal: Int
    val balance: BigDecimal

    val balanceUpdatedSignal: PublishSubject<Unit>
    val lastBlockHeightUpdatedSignal: PublishSubject<Unit>
    val adapterStateUpdatedSubject: PublishSubject<Unit>
    val transactionRecordsSubject: PublishSubject<List<TransactionRecord>>

    val state: AdapterState
    val confirmationsThreshold: Int
    val lastBlockHeight: Int?

    val debugInfo: String

    fun start()
    fun stop()
    fun refresh()
    fun clear()

    fun parsePaymentAddress(address: String): PaymentRequestAddress

    fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> (Unit))? = null)
    fun availableBalance(address: String?): BigDecimal

    fun fee(value: BigDecimal, address: String?): BigDecimal

    @Throws
    fun validate(address: String)

    fun validate(amount: BigDecimal, address: String?): List<SendStateError>

    val receiveAddress: String
    fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>>
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
    val lockStateUpdatedSignal: PublishSubject<Unit>
    var isLocked: Boolean
    fun lock()
    fun onUnlock()
    fun didEnterBackground()
    fun willEnterForeground()
}

interface IAppConfigProvider {
    val ipfsUrl: String
    val fiatDecimal: Int
    val maxDecimal: Int
    val testMode: Boolean
    val localizations: List<String>
    val currencies: List<Currency>
    val defaultCoins: List<Coin>
    val erc20tokens: List<Coin>
}

interface IOneTimerDelegate {
    fun onFire()
}

interface IRateStorage {
    fun latestRateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>
    fun rateObservable(coinCode: CoinCode, currencyCode: String, timestamp: Long): Flowable<List<Rate>>
    fun save(rate: Rate)
    fun saveLatest(rate: Rate)
    fun deleteAll()
    fun zeroRatesObservable(currencyCode: String): Single<List<Rate>>
}

interface ICoinStorage {
    fun enabledCoinsObservable(): Flowable<List<Coin>>
    fun save(coins: List<Coin>)
    fun update(inserted: List<Coin>, deleted: List<Coin>)
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

interface ICoinManager {
    val coinsUpdatedSignal: PublishSubject<Unit>
    var coins: List<Coin>
    val allCoins: List<Coin>
    fun enableDefaultCoins()
    fun clear()
}

interface IAppNumberFormatter {
    fun format(coinValue: CoinValue, explicitSign: Boolean = false, realNumber: Boolean = false): String?
    fun formatForTransactions(coinValue: CoinValue): String?
    fun format(currencyValue: CurrencyValue, showNegativeSign: Boolean = true, realNumber: Boolean = false, canUseLessSymbol: Boolean = true): String?
    fun formatForTransactions(currencyValue: CurrencyValue, isIncoming: Boolean): SpannableString
    fun format(value: Double): String
}

sealed class Error : Exception() {
    class CoinTypeException : Error()
}

sealed class SendStateError {
    object InsufficientAmount: SendStateError()
    object InsufficientFeeBalance: SendStateError()
}
