package io.horizontalsystems.core

import android.app.Application
import android.content.Context
import io.horizontalsystems.core.helpers.LocaleHelper
import java.util.Locale

abstract class CoreApp : Application() {

    companion object : ICoreApp {
        override lateinit var backgroundManager: BackgroundManager
        override lateinit var encryptionManager: IEncryptionManager
        override lateinit var systemInfoManager: ISystemInfoManager
        override lateinit var keyStoreManager: IKeyStoreManager
        override lateinit var keyProvider: IKeyProvider
        override lateinit var pinComponent: IPinComponent
        override lateinit var pinSettingsStorage: IPinSettingsStorage
        override lateinit var lockoutStorage: ILockoutStorage
        override lateinit var thirdKeyboardStorage: IThirdKeyboard

        override lateinit var instance: CoreApp
    }

    abstract fun localizedContext(): Context
    abstract fun getApplicationSignatures(): List<ByteArray>

    fun localeAwareContext(base: Context): Context {
        return LocaleHelper.onAttach(base)
    }

    fun getLocale(): Locale {
        return LocaleHelper.getLocale(this)
    }

    fun setLocale(currentLocale: Locale) {
        LocaleHelper.setLocale(this, currentLocale)
    }

    fun isLocaleRTL(): Boolean {
        return LocaleHelper.isRTL(Locale.getDefault())
    }
}
