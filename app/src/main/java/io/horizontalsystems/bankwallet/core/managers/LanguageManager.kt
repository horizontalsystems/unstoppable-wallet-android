package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import java.util.*


class LanguageManager(private val localStorage: ILocalStorage, fallbackLanguage: Locale) : ILanguageManager {

    override var preferredLanguage: Locale? = null
        get() {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> App.instance.resources.configuration.locales.get(0)
                else -> App.instance.resources.configuration.locale
            }
        }

    override var availableLanguages = arrayOf("de", "en", "ky", "ru").map { Locale(it) }

    private var language: Locale = localStorage.currentLanguage?.let { Locale(localStorage.currentLanguage) }
            ?: preferredLanguage ?: fallbackLanguage

    override var currentLanguage: Locale
        get() = language
        set(value) {
            language = value
            localStorage.currentLanguage = value.language

            val configuration = App.instance.resources.configuration
            configuration.setLocale(value)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                App.instance.createConfigurationContext(configuration)
            } else {
                val displayMetrics = App.instance.resources.displayMetrics
                App.instance.resources.updateConfiguration(configuration, displayMetrics)
            }
        }

}
