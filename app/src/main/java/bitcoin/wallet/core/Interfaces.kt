package bitcoin.wallet.core

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Flowable

interface ILocalStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
    fun clearAll()
    fun savePin(pin: String)
    fun getPin(): String?
    fun wordlistBackedUp(backedUp: Boolean)
    fun isWordListBackedUp(): Boolean
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getLatestRate(coinCode:String, currency: String): Flowable<Double>
    fun getRate(coinCode:String, currency: String, year: Int, month: String, day: String, hour: String, minute: String): Flowable<Double>
    fun getRateByDay(coinCode:String, currency: String, year: Int, month: String, day: String): Flowable<Double>
}

interface IExchangeRateManager {
    fun refreshRates()
    fun getRate(coinCode:String, currency: String, timestamp: Long): Flowable<Double>
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
}
