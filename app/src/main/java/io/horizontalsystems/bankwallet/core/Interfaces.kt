package io.horizontalsystems.bankwallet.core

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.coins.Coin
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

interface ILocalStorage {
    var currentLanguage: String?
    var isBackedUp: Boolean
    var isBiometricOn: Boolean
    var isLightModeOn: Boolean
    var iUnderstand: Boolean
    var unlockAttemptsLeft: Int
    var baseCurrencyCode: String?
    var blockTillDate: Long?
    fun clearAll()
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
    fun getLatestRate(coinCode: String, currency: String): Flowable<Double>
    fun getRate(coinCode: String, currency: String, year: Int, month: String, day: String, hour: String, minute: String): Flowable<Double>
    fun getRateByDay(coinCode: String, currency: String, year: Int, month: String, day: String): Flowable<Double>
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

interface IExchangeRateManager {
    fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double>
    fun getExchangeRates(): MutableMap<Coin, CurrencyValue>
    fun getLatestExchangeRateSubject(): PublishSubject<MutableMap<Coin, CurrencyValue>>
}

interface IKeyStoreSafeExecute {
    fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
}

interface IAdapterManager {
    var adapters: MutableList<IAdapter>
    var subject: PublishSubject<Boolean>
    fun start()
    fun refresh()
    fun clear()
}

interface IWordsManager {
    fun safeLoad()
    var words: List<String>?
    var isBackedUp: Boolean
    var isLoggedIn: Boolean
    var backedUpSubject: PublishSubject<Boolean>
    fun createWords()
    fun validate(words: List<String>)
    fun restore(words: List<String>)
    fun removeWords()
}

interface ILanguageManager {
    var currentLanguage: Locale
    var preferredLanguage: Locale?
    val availableLanguages: List<Locale>
}

interface IAdapter {
    val id: String
    val coin: Coin
    val balance: Double

    val balanceSubject: PublishSubject<Double>
    val progressSubject: BehaviorSubject<Double>

    val latestBlockHeight: Int
    val latestBlockHeightSubject: PublishSubject<Any>

    val transactionRecords: List<TransactionRecord>
    val transactionRecordsSubject: PublishSubject<Any>

    val receiveAddress: String

    fun debugInfo()

    fun start()
    fun refresh()
    fun clear()

    fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))? = null)
    fun fee(value: Int, senderPay: Boolean): Double
    fun validate(address: String): Boolean
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
    val enabledCoins: List<String>
    val localizations: List<String>
    val currencies: List<Currency>
}
