package bitcoin.wallet.core.managers

import android.content.SharedPreferences
import android.text.TextUtils
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IEncryptionManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.entities.Currency
import com.google.gson.Gson
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable


class PreferencesManager(private val encryptionManager: IEncryptionManager) : ILocalStorage, ISettingsManager {

    override val savedWords: List<String>?
        get() {
            val string = App.preferences.getString(MNEMONIC_WORDS, null)
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string).split(" ").filter { it.isNotBlank() }
            }
        }

    override fun saveWords(words: List<String>) {
        App.preferences.edit().putString(MNEMONIC_WORDS, encryptionManager.encrypt(words.joinToString(" "))).apply()
    }

    override fun clearAll() {
        App.preferences.edit().clear().apply()
    }

    private val MNEMONIC_WORDS = "mnemonicWords"
    private val LOCK_PIN = "lockPin"
    private val LIGHT_MODE_ENABLED = "light_mode_enabled"
    private val FINGERPRINT_ENABLED = "fingerprint_enabled"
    private val WORDLIST_BACKUP = "wordlist_backup"
    private val BASE_CURRENCY = "base_currency"

    override fun isLightModeEnabled() = App.preferences.getBoolean(LIGHT_MODE_ENABLED, false)

    override fun setLightModeEnabled(enabled: Boolean) {
        App.preferences.edit().putBoolean(LIGHT_MODE_ENABLED, enabled).apply()
    }

    override fun isFingerprintEnabled() = App.preferences.getBoolean(FINGERPRINT_ENABLED, false)

    override fun setFingerprintEnabled(enabled: Boolean) {
        App.preferences.edit().putBoolean(FINGERPRINT_ENABLED, enabled).apply()
    }

    override fun savePin(pin: String) {
        App.preferences.edit().putString(LOCK_PIN, encryptionManager.encrypt(pin)).apply()
    }

    override fun getPin(): String? {
        val string = App.preferences.getString(LOCK_PIN, null)
        return if (TextUtils.isEmpty(string)) {
            null
        } else {
            encryptionManager.decrypt(string)
        }
    }

    override fun wordlistBackedUp(backedUp: Boolean) {
        App.preferences.edit().putBoolean(WORDLIST_BACKUP, backedUp).apply()
    }

    override fun isWordListBackedUp(): Boolean {
        return App.preferences.getBoolean(WORDLIST_BACKUP, false)
    }

    fun setBaseCurrency(currency: Currency) {
        val gson = Gson()
        val json = gson.toJson(currency)
        App.preferences.edit().putString(BASE_CURRENCY, json).apply()
    }

    fun getBaseCurrency(): Currency {
        val gson = Gson()
        val json = App.preferences.getString(BASE_CURRENCY, "")
        return if (json?.isBlank() == true) defaultCurrency else gson.fromJson<Currency>(json, Currency::class.java)
    }

    fun getBaseCurrencyFlowable(): Flowable<Currency> =
            Flowable.create({ emitter ->
                val emitSavedBaseCurrency = {
                    val currency = getBaseCurrency()
                    emitter.onNext(currency)
                }

                val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, updatedKey ->
                    if (TextUtils.equals(BASE_CURRENCY, updatedKey)) {
                        emitSavedBaseCurrency()
                    }
                }

                App.preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
                emitSavedBaseCurrency()
                emitter.setCancellable { App.preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener) }
            }, BackpressureStrategy.LATEST)

    private val defaultCurrency: Currency = Currency().apply {
        code = "USD"
        symbol = "$"
        description = "United States Dollar"
    }

}
