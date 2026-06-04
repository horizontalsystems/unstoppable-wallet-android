package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.horizontalsystems.core.helpers.LocaleHelper
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val fallbackLocale by LocaleHelper::fallbackLocale

    var currentLocale: Locale = LocaleHelper.getLocale(context)
        set(value) {
            field = value

            LocaleHelper.setLocale(context, currentLocale)
        }

    var currentLocaleTag: String
        get() = currentLocale.toLanguageTag()
        set(value) {
            currentLocale = Locale.forLanguageTag(value)
        }

    val currentLanguageName: String
        get() = getName(currentLocaleTag)

    val currentLanguage: String
        get() = currentLocale.language

    fun getName(tag: String): String {
        return Locale.forLanguageTag(tag)
            .getDisplayName(currentLocale)
            .replaceFirstChar(Char::uppercase)
    }

    fun getNativeName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar(Char::uppercase)
    }

}
