package bitcoin.wallet.core

import android.hardware.fingerprint.FingerprintManager
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

interface ILocalStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
    fun clearAll()
    fun savePin(pin: String)
    fun getPin(): String?
    fun wordListBackedUp(backedUp: Boolean)
    fun isWordListBackedUp(): Boolean
    fun pinIsEmpty(): Boolean
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getLatestRate(coinCode:String, currency: String): Flowable<Double>
    fun getRate(coinCode:String, currency: String, year: Int, month: String, day: String, hour: String, minute: String): Flowable<Double>
    fun getRateByDay(coinCode:String, currency: String, year: Int, month: String, day: String): Flowable<Double>
    fun getCurrencies(): Flowable<List<Currency>>
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): FingerprintManager.CryptoObject?
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
}

interface ISettingsManager {
    fun isFingerprintEnabled(): Boolean
    fun setFingerprintEnabled(enabled: Boolean)

    fun isLightModeEnabled(): Boolean
    fun setLightModeEnabled(enabled: Boolean)

    fun setBaseCurrency(currency: Currency)
    fun getBaseCurrency(): Currency
}

interface ICurrencyManager {
    fun getBaseCurrencyFlowable(): Flowable<Currency>
}

interface IExchangeRateManager {
    fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double>
    fun getExchangeRates(): MutableMap<Coin, CurrencyValue>
    fun getLatestExchangeRateSubject(): PublishSubject<MutableMap<Coin, CurrencyValue>>
}

interface IKeyStoreSafeExecute {
    fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
}
