package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.send.SendModule


class LocalStorageManager : ILocalStorage {

    private val CURRENT_LANGUAGE = "current_language"
    private val LIGHT_MODE_ENABLED = "light_mode_enabled"
    private val FINGERPRINT_ENABLED = "fingerprint_enabled"
    private val SEND_INPUT_TYPE = "send_input_type"
    private val WORDLIST_BACKUP = "wordlist_backup"
    private val I_UNDERSTAND = "i_understand"
    private val BLOCK_TILL_DATE = "unblock_date"
    private val BASE_CURRENCY_CODE = "base_currency_code"
    private val NEW_WALLET = "new_wallet"
    private val FAILED_ATTEMPTS = "failed_attempts"
    private val LOCKOUT_TIMESTAMP = "lockout_timestamp"
    private val BASE_BITCOIN_PROVIDER = "base_bitcoin_provider"
    private val BASE_ETHEREUM_PROVIDER = "base_ethereum_provider"

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

    override var sendInputType: SendModule.InputType?
        get() = App.preferences.getString(SEND_INPUT_TYPE, null)?.let {
            try {
                SendModule.InputType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            val editor = App.preferences.edit()
            when (value) {
                null -> editor.remove(SEND_INPUT_TYPE).apply()
                else -> editor.putString(SEND_INPUT_TYPE, value.name).apply()
            }
        }

    override var isLightModeOn: Boolean
        get() = App.preferences.getBoolean(LIGHT_MODE_ENABLED, false)
        set(enabled) {
            App.preferences.edit().putBoolean(LIGHT_MODE_ENABLED, enabled).apply()
        }

    override var iUnderstand: Boolean
        get() = App.preferences.getBoolean(I_UNDERSTAND, false)
        set(value) {
            App.preferences.edit().putBoolean(I_UNDERSTAND, value).apply()
        }

    override var baseCurrencyCode: String?
        get() = App.preferences.getString(BASE_CURRENCY_CODE, null)
        set(value) {
            App.preferences.edit().putString(BASE_CURRENCY_CODE, value).apply()
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

    override var isNewWallet: Boolean
        get() = App.preferences.getBoolean(NEW_WALLET, false)
        set(value) {
            App.preferences.edit().putBoolean(NEW_WALLET, value).apply()
        }

    override var failedAttempts: Int?
        get() {
            val attempts = App.preferences.getInt(FAILED_ATTEMPTS, 0)
            return when (attempts) {
                0 -> null
                else -> attempts
            }
        }
        set(value) {
            value?.let {
                App.preferences.edit().putInt(FAILED_ATTEMPTS, it).apply()
            } ?: App.preferences.edit().remove(FAILED_ATTEMPTS).apply()
        }

    override var lockoutUptime: Long?
        get() {
            val timestamp = App.preferences.getLong(LOCKOUT_TIMESTAMP, 0L)
            return when (timestamp) {
                0L -> null
                else -> timestamp
            }
        }
        set(value) {
            value?.let {
                App.preferences.edit().putLong(LOCKOUT_TIMESTAMP, it).apply()
            } ?: App.preferences.edit().remove(LOCKOUT_TIMESTAMP).apply()
        }

    override var baseBitcoinProvider: String?
        get() = App.preferences.getString(BASE_BITCOIN_PROVIDER, null)
        set(value) {
            App.preferences.edit().putString(BASE_BITCOIN_PROVIDER, value).apply()
        }

    override var baseEthereumProvider: String?
        get() = App.preferences.getString(BASE_ETHEREUM_PROVIDER, null)
        set(value) {
            App.preferences.edit().putString(BASE_ETHEREUM_PROVIDER, value).apply()
        }

    override fun clear() {
        App.preferences.edit().clear().apply()
    }

}
