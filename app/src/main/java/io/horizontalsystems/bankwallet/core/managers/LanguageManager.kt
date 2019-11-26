package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILanguageManager
import java.util.*

class LanguageManager(private val appConfigProvider: IAppConfigProvider, fallbackLanguage: String) : ILanguageManager {

    override var currentLocale: Locale = App.instance.getLocale()
        set(value) {
            field = value

            App.instance.setLocale(currentLocale)
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
