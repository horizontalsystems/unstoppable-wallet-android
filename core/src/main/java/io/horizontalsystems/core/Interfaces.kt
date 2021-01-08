package io.horizontalsystems.core

import android.app.Activity
import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.security.KeyStoreValidationResult
import io.reactivex.Flowable
import io.reactivex.Observable
import java.util.*
import javax.crypto.SecretKey

interface ICoreApp {
    var preferences: SharedPreferences
    var buildConfigProvider: IBuildConfigProvider
    var languageConfigProvider: ILanguageConfigProvider
    var backgroundManager: BackgroundManager
    var encryptionManager: IEncryptionManager
    var systemInfoManager: ISystemInfoManager
    var languageManager: ILanguageManager
    var currencyManager: ICurrencyManager
    var keyStoreManager: IKeyStoreManager
    var keyProvider: IKeyProvider
    var pinComponent: IPinComponent
    var pinStorage: IPinStorage
    var themeStorage: IThemeStorage
    var thirdKeyboardStorage: IThirdKeyboard
    var instance: CoreApp
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): BiometricPrompt.CryptoObject?
}

interface IBuildConfigProvider {
    val testMode: Boolean
    val skipRootCheck: Boolean
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

interface IPinComponent {
    var isBiometricAuthEnabled: Boolean
    val isPinSet: Boolean
    val isLocked: Boolean
    val pinSetFlowable: Flowable<Unit>

    fun willEnterForeground(activity: Activity)
    fun didEnterBackground()
    fun updateLastExitDateBeforeRestart()
    fun store(pin: String)
    fun validate(pin: String): Boolean
    fun clear()
    fun onUnlock()
    fun shouldShowPin(activity: Activity): Boolean
}

interface ILanguageManager {
    var fallbackLocale: Locale
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
    var appLastVisitTime: Long
    var biometricAuthEnabled: Boolean
    var pin: String?

    fun clearPin()
}

interface IThemeStorage {
    var isLightModeOn: Boolean
}

interface IThirdKeyboard {
    var isThirdPartyKeyboardAllowed: Boolean
}

interface IKeyStoreManager {
    fun validateKeyStore(): KeyStoreValidationResult
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

interface ICurrentDateProvider {
    val currentDate: Date
}
