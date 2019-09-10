package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import java.util.*

class LanguageManager(private val localStorage: ILocalStorage, fallbackLanguage: String) : ILanguageManager {

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

    override fun getName(language: String): String {
        return Locale(language).displayLanguage
    }

    override fun getNativeName(language: String): String {
        val locale = Locale(language)
        return locale.getDisplayLanguage(locale)
    }

    private val preferredSystemLocale: Locale?
        get() {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> App.instance.resources.configuration.locales.get(0)
                else -> App.instance.resources.configuration.locale
            }
        }

}
