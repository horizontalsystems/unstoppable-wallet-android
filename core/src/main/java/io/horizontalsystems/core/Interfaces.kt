package io.horizontalsystems.core

import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import java.util.*
import javax.crypto.SecretKey

interface ICoreApp {
    var preferences: SharedPreferences
    var appConfigTestMode: IAppConfigTestMode
    var languageConfigProvider: ILanguageConfigProvider
    var encryptionManager: IEncryptionManager
    var systemInfoManager: ISystemInfoManager
    var languageManager: ILanguageManager
    var currencyManager: ICurrencyManager
    var lockManager: ILockManager
    var keyStoreManager: IKeyStoreManager
    var keyProvider: IKeyProvider
    var secureStorage: ISecuredStorage
    var pinManager: IPinManager
    var pinStorage: IPinStorage
    var themeStorage: IThemeStorage

    var instance: CoreApp
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): BiometricPrompt.CryptoObject?
}

interface IAppConfigTestMode {
    val testMode: Boolean
}

interface ILanguageConfigProvider {
    val localizations: List<String>
}

interface ISystemInfoManager {
    val appVersion: String
    val isSystemLockOff: Boolean
    val biometricAuthSupported: Boolean
    val deviceModel: String
    val osVersion: String
}

interface ISecuredStorage {
    var isFingerprintEnabled: Boolean
    val savedPin: String?
    fun savePin(pin: String)
    fun removePin()
    fun pinIsEmpty(): Boolean
}

interface IPinManager {
    var isFingerprintEnabled: Boolean
    val isPinSet: Boolean

    fun store(pin: String)
    fun validate(pin: String): Boolean
    fun clear()
}

interface ILanguageManager {
    var currentLocale: Locale
    var currentLanguage: String
    val currentLanguageName: String

    fun getName(language: String): String
    fun getNativeName(language: String): String
}

interface ICurrencyManager {
    var baseCurrency: Currency
    val baseCurrencyUpdatedSignal: Observable<Unit>
    val currencies: List<Currency>
}

interface IPinStorage {
    var failedAttempts: Int?
    var lockoutUptime: Long?
}

interface IThemeStorage {
    var isLightModeOn: Boolean
}

interface IKeyStoreManager {
    val isKeyInvalidated: Boolean
    val isUserNotAuthenticated: Boolean

    fun removeKey()
    fun resetApp()
}

interface IKeyStoreCleaner {
    var encryptedSampleText: String?
    fun cleanApp()
}

interface IKeyProvider {
    fun getKey(): SecretKey
}

interface ILockManager {
    var isLocked: Boolean
    fun onUnlock()
}

interface ICurrentDateProvider {
    val currentDate: Date
}
