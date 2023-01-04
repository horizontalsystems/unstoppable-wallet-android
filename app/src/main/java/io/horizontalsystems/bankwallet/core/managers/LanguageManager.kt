package io.horizontalsystems.bankwallet.core.managers

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.ILanguageManager
import java.util.*

class LanguageManager : ILanguageManager {

    override val fallbackLocale: Locale = Locale.ENGLISH

    private val appLocaleSetInSystemSettings: Locale
        get() = AppCompatDelegate.getApplicationLocales().get(0) ?: fallbackLocale

    override val currentLocale: Locale
        get() {
            val locales = supportedLocales.map { it.locale }
            return locales.firstOrNull { it == appLocaleSetInSystemSettings }
                ?: locales.firstOrNull { it.language == appLocaleSetInSystemSettings.language }
                ?: fallbackLocale
        }

    override val currentLanguageName: String
        get() = currentLocale.getDisplayName(currentLocale).replaceFirstChar(Char::uppercase)

    override val currentLanguage: String
        get() = currentLocale.language

    override fun setLocale(locale: Locale) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
    }

    data class SupportedLocale(val locale: Locale, @DrawableRes val icon: Int) {
        fun name(currentLocale: Locale): String
             = locale.getDisplayName(currentLocale).replaceFirstChar(Char::uppercase)

        val nativeName: String
            get() = locale.getDisplayName(locale).replaceFirstChar(Char::uppercase)
    }

    companion object {
        val supportedLocales = listOf(
            SupportedLocale(Locale("de"), R.drawable.icon_24_flags_germany),
            SupportedLocale(Locale("en"), R.drawable.icon_24_flags_england),
            SupportedLocale(Locale("es"), R.drawable.icon_24_flags_spain),
            SupportedLocale(Locale("pt"), R.drawable.icon_24_flags_brazil),
            SupportedLocale(Locale("fa"), R.drawable.icon_24_flags_iran),
            SupportedLocale(Locale("fr"), R.drawable.icon_24_flags_france),
            SupportedLocale(Locale("ko"), R.drawable.icon_24_flags_korea),
            SupportedLocale(Locale("ru"), R.drawable.icon_24_flags_russia),
            SupportedLocale(Locale("tr"), R.drawable.icon_24_flags_turkey),
            SupportedLocale(Locale("zh"), R.drawable.icon_24_flags_china)
        )
    }
}
