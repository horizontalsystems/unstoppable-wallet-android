package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import java.util.*

class LanguageManager(private val localStorage: ILocalStorage, private val appConfigProvider: IAppConfigProvider, fallbackLanguage: String) : ILanguageManager {

    override var currentLocale: Locale = localStorage.currentLanguage?.let { Locale(it) } ?: preferredSystemLocale ?: Locale(fallbackLanguage)
        set(value) {
            field = value

            localStorage.currentLanguage = value.language

            val configuration = App.instance.resources.configuration
            configuration.setLocale(currentLocale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                App.instance.createConfigurationContext(configuration)
            } else {
                val displayMetrics = App.instance.resources.displayMetrics
                App.instance.resources.updateConfiguration(configuration, displayMetrics)
            }
        }

    override var currentLanguage: String
        get() = currentLocale.language
        set(value) {
            currentLocale = Locale(value)
        }

    override val currentLanguageName: String
        get() = currentLocale.displayLanguage.capitalize()

    override fun getName(language: String): String {
        return Locale(language).displayLanguage.capitalize()
    }

    override fun getNativeName(language: String): String {
        val locale = Locale(language)
        return locale.getDisplayLanguage(locale).capitalize()
    }

    private val preferredSystemLocale: Locale?
        get() {
            val appLocaleLanguages = appConfigProvider.localizations.map { Locale(it).language }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val deviceLocales = App.instance.resources.configuration.locales

                for (i in 0 until deviceLocales.size()) {
                    val deviceLocale = deviceLocales.get(i)

                    if (appLocaleLanguages.contains(deviceLocale.language)) {
                        return deviceLocale
                    }
                }
            } else {
                val deviceLocale = App.instance.resources.configuration.locale
                if (appLocaleLanguages.contains(deviceLocale.language)) {
                    return deviceLocale
                }
            }
            return null
        }

}
