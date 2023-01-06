package io.horizontalsystems.bankwallet.core.managers

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.horizontalsystems.bankwallet.R
import java.util.*

class LanguageManager {

    val fallbackLocale: Locale = Locale.ENGLISH

    private val appLocaleSetInSystemSettings: Locale
        get() = AppCompatDelegate.getApplicationLocales().get(0) ?: fallbackLocale

    val currentLocale: Locale
        get() {
            val locales = supportedLocales.map { it.locale }
            return locales.firstOrNull { it == appLocaleSetInSystemSettings }
                ?: locales.firstOrNull { it.language == appLocaleSetInSystemSettings.language }
                ?: fallbackLocale
        }

    val currentLanguageName: String
        get() = currentLocale.getDisplayName(currentLocale).replaceFirstChar(Char::uppercase)

    val currentLanguage: String
        get() = currentLocale.language

    fun setLocale(locale: Locale) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
    }

    data class SupportedLocale(val locale: Locale, @DrawableRes val icon: Int) {
        fun name(currentLocale: Locale): String =
            locale.getDisplayName(currentLocale).replaceFirstChar(Char::uppercase)

        val nativeName: String
            get() = locale.getDisplayName(locale).replaceFirstChar(Char::uppercase)
    }

    companion object {
        val supportedLocales = listOf(
            SupportedLocale(Locale("de"), R.drawable.icon_32_flag_germany),
            SupportedLocale(Locale("en"), R.drawable.icon_32_flag_england),
            SupportedLocale(Locale("es"), R.drawable.icon_32_flag_spain),
            SupportedLocale(Locale("pt"), R.drawable.icon_32_flag_brazil),
            SupportedLocale(Locale("fa"), R.drawable.icon_32_flag_iran),
            SupportedLocale(Locale("fr"), R.drawable.icon_32_flag_france),
            SupportedLocale(Locale("ko"), R.drawable.icon_32_flag_korea),
            SupportedLocale(Locale("ru"), R.drawable.icon_32_flag_russia),
            SupportedLocale(Locale("tr"), R.drawable.icon_32_flag_turkey),
            SupportedLocale(Locale("zh"), R.drawable.icon_32_flag_china)
        )
    }
}
