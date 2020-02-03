package io.horizontalsystems.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.horizontalsystems.core.helpers.LocaleHelper
import java.util.*

abstract class CoreApp : Application() {
    companion object : ICoreApp {
        override lateinit var preferences: SharedPreferences
        override lateinit var appConfigTestMode: IAppConfigTestMode
        override lateinit var encryptionManager: IEncryptionManager
        override lateinit var systemInfoManager: ISystemInfoManager
        override lateinit var lockManager: ILockManager
        override lateinit var pinManager: IPinManager
        override lateinit var pinStorage: IPinStorage
        override lateinit var themeStorage: IThemeStorage

        override lateinit var instance: CoreApp
    }

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
