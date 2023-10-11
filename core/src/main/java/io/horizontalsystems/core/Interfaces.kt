package io.horizontalsystems.core

import android.app.Activity
import io.horizontalsystems.core.security.KeyStoreValidationResult
import io.reactivex.Flowable
import java.util.Date
import javax.crypto.SecretKey

interface ICoreApp {
    var backgroundManager: BackgroundManager
    var encryptionManager: IEncryptionManager
    var systemInfoManager: ISystemInfoManager
    var keyStoreManager: IKeyStoreManager
    var keyProvider: IKeyProvider
    var pinComponent: IPinComponent
    var pinSettingsStorage: IPinSettingsStorage
    var lockoutStorage: ILockoutStorage
    var thirdKeyboardStorage: IThirdKeyboard
    var instance: CoreApp
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
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
    fun setPin(pin: String)
    fun setDuressPin(pin: String)
    fun disablePin()
    fun disableDuressPin()
    fun isDuressPinSet(): Boolean
    fun unlock(pin: String): Boolean
    fun validateCurrentLevel(pin: String): Boolean
    fun onBiometricUnlock()
    fun initDefaultPinLevel()
    fun lock()
    fun updateLastExitDateBeforeRestart()
    fun shouldShowPin(activity: Activity): Boolean
    fun isUnique(pin: String, forDuress: Boolean): Boolean
    fun keepUnlocked()
}

interface ILockoutStorage {
    var failedAttempts: Int?
    var lockoutUptime: Long?
}

interface IPinSettingsStorage {
    var biometricAuthEnabled: Boolean
    var pin: String?
}

interface IThirdKeyboard {
    var isThirdPartyKeyboardAllowed: Boolean
}

interface IKeyStoreManager {
    fun validateKeyStore(): KeyStoreValidationResult
    fun removeKey()
    fun resetApp(reason: String)
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
