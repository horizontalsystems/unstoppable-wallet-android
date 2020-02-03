package io.horizontalsystems.core

import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import java.util.*

interface ICoreApp {
    var preferences: SharedPreferences
    var appConfigTestMode: IAppConfigTestMode
    var encryptionManager: IEncryptionManager
    var systemInfoManager: ISystemInfoManager
    var lockManager: ILockManager
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

interface ISystemInfoManager {
    val appVersion: String
    val isSystemLockOff: Boolean
    val biometricAuthSupported: Boolean
    val deviceModel: String
    val osVersion: String
}

interface IPinManager {
    var isFingerprintEnabled: Boolean
    val isPinSet: Boolean

    fun store(pin: String)
    fun validate(pin: String): Boolean
    fun clear()
}

interface IPinStorage {
    var failedAttempts: Int?
    var lockoutUptime: Long?
}

interface IThemeStorage {
    var isLightModeOn: Boolean
}

interface ILockManager {
    var isLocked: Boolean
    fun onUnlock()
}

interface ICurrentDateProvider {
    val currentDate: Date
}
