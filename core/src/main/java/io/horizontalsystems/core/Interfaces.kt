package io.horizontalsystems.core

import android.app.Activity
import io.horizontalsystems.core.security.KeyStoreValidationResult
import io.reactivex.Flowable
import java.util.*
import javax.crypto.SecretKey

interface ICoreApp {
    var backgroundManager: BackgroundManager
    var encryptionManager: IEncryptionManager
    var systemInfoManager: ISystemInfoManager
    var keyStoreManager: IKeyStoreManager
    var keyProvider: IKeyProvider
    var pinComponent: IPinComponent
    var pinStorage: IPinStorage
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
    fun updateLastExitDateBeforeRestart()
    fun store(pin: String)
    fun validate(pin: String): Boolean
    fun clear()
    fun onUnlock()
    fun shouldShowPin(activity: Activity): Boolean
    fun lock()
}

interface IPinStorage {
    var failedAttempts: Int?
    var lockoutUptime: Long?
    var biometricAuthEnabled: Boolean
    var pin: String?

    fun clearPin()
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
