package bitcoin.wallet.core.managers

import bitcoin.wallet.core.App
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
import com.google.gson.Gson


class LocalStorageManager : ILocalStorage {

    private val CURRENT_LANGUAGE = "current_language"
    private val LIGHT_MODE_ENABLED = "light_mode_enabled"
    private val FINGERPRINT_ENABLED = "fingerprint_enabled"
    private val WORDLIST_BACKUP = "wordlist_backup"
    private val I_UNDERSTAND = "i_understand"
    private val UNLOCK_PIN_ATTEMPTS_LEFT = "unlock_pin_attempts_left"
    private val BLOCK_TILL_DATE = "unblock_date"


    override var currentLanguage: String?
        get() = App.preferences.getString(CURRENT_LANGUAGE, null)
        set(language) {
            App.preferences.edit().putString(CURRENT_LANGUAGE, language).apply()
        }

    override var isBackedUp: Boolean
        get() = App.preferences.getBoolean(WORDLIST_BACKUP, false)
        set(backedUp) {
            App.preferences.edit().putBoolean(WORDLIST_BACKUP, backedUp).apply()
        }

    override var isBiometricOn: Boolean
        get() = App.preferences.getBoolean(FINGERPRINT_ENABLED, false)
        set(enabled) {
            App.preferences.edit().putBoolean(FINGERPRINT_ENABLED, enabled).apply()
        }

    override var isLightModeOn: Boolean
        get() = App.preferences.getBoolean(LIGHT_MODE_ENABLED, false)
        set(enabled) {
            App.preferences.edit().putBoolean(LIGHT_MODE_ENABLED, enabled).apply()
        }

    override var iUnderstand: Boolean
        get() = App.preferences.getBoolean(I_UNDERSTAND, false)
        set(value) {
            App.preferences.edit().putBoolean(LIGHT_MODE_ENABLED, value).apply()
        }

    override var unlockAttemptsLeft: Int
        get() = App.preferences.getInt(UNLOCK_PIN_ATTEMPTS_LEFT, 5)
        set(value) {
            App.preferences.edit().putInt(UNLOCK_PIN_ATTEMPTS_LEFT, value).apply()
        }

    override var baseCurrency: Currency
        get() {
            val gson = Gson()
            val json = App.preferences.getString(BASE_CURRENCY, "")
            return if (json?.isBlank() == true) defaultCurrency else gson.fromJson<Currency>(json, Currency::class.java)
        }
        set(currency) {
            val gson = Gson()
            val json = gson.toJson(currency)
            App.preferences.edit().putString(BASE_CURRENCY, json).apply()
        }

    override var blockTillDate: Long?
        get() {
            val date = App.preferences.getLong(BLOCK_TILL_DATE, 0)
            return if (date > 0) date else null
        }
        set(date) {
            date?.let {
                App.preferences.edit().putLong(BLOCK_TILL_DATE, date).apply()
            } ?: run {
                App.preferences.edit().remove(BLOCK_TILL_DATE).apply()
            }
        }

    override fun clearAll() {
        App.preferences.edit().clear().apply()
    }

    private val defaultCurrency: Currency = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }

    companion object {
        val BASE_CURRENCY = "base_currency"
    }

}
