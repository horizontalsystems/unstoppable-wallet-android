package bitcoin.wallet.core

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable

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
    fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String>
    fun getExchangeRates(): Observable<Map<String, Double>>
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
