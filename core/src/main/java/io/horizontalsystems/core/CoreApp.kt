package io.horizontalsystems.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.horizontalsystems.core.helpers.LocaleHelper
import java.util.*

abstract class CoreApp : Application() {

    companion object : ICoreApp {
        override lateinit var preferences: SharedPreferences
        override lateinit var buildConfigProvider: IBuildConfigProvider
        override lateinit var languageConfigProvider: ILanguageConfigProvider
        override lateinit var backgroundManager: BackgroundManager
        override lateinit var encryptionManager: IEncryptionManager
        override lateinit var systemInfoManager: ISystemInfoManager
        override lateinit var languageManager: ILanguageManager
        override lateinit var currencyManager: ICurrencyManager
        override lateinit var keyStoreManager: IKeyStoreManager
        override lateinit var keyProvider: IKeyProvider
        override lateinit var pinComponent: IPinComponent
        override lateinit var pinStorage: IPinStorage
        override lateinit var themeStorage: IThemeStorage
        override lateinit var thirdKeyboardStorage: IThirdKeyboard

        override lateinit var instance: CoreApp
    }

    abstract fun localizedContext(): Context

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
