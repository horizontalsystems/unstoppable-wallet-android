package cash.p.terminal.strings.helpers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

// https://github.com/zeugma-solutions/locale-helper-android

object LocaleHelper {

    val fallbackLocale: Locale = Locale.ENGLISH

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private val supportedLanguageTags: Set<String> by lazy {
        LocaleType.values().mapTo(hashSetOf()) { it.tag }
    }
    private val RTL: Set<String> by lazy {
        hashSetOf(
            "ar",
            "dv",
            "fa",
            "ha",
            "he",
            "iw",
            "ji",
            "ps",
            "sd",
            "ug",
            "ur",
            "yi"
        )
    }

    fun onAttach(context: Context): Context {
        val locale = getLocale(context)
        return updateContextLocale(context, locale)
    }

    fun getLocale(context: Context): Locale {
        val storedLocale = getStoredLocale(context)
        storedLocale?.let {
            return storedLocale
        }

        return getSystemLocale(context)
    }

    private fun getSystemLocale(context: Context): Locale {
        val tag = context.resources.configuration.locales.get(0).supportedLanguageTag()

        //use system locale if it is supported by app, else use fallback locale
        if (supportedLanguageTags.contains(tag)) {
            val localeFromSupportedTag = Locale.forLanguageTag(tag)
            persist(context, localeFromSupportedTag)
            return localeFromSupportedTag
        }
        return fallbackLocale
    }

    fun setLocale(context: Context, locale: Locale) {
        persist(context, locale)

        updateContextLocale(context, locale)
    }

    fun isRTL(locale: Locale): Boolean {
        return RTL.contains(locale.language)
    }

    private fun getStoredLocale(context: Context): Locale? {
        val preferences = getPreferences(context)
        val languageTag = preferences.getString(SELECTED_LANGUAGE, null)
        return languageTag?.let { Locale.forLanguageTag(it) }
    }

    private fun updateContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val currentConfiguration = context.resources.configuration
        if (currentConfiguration.locales.get(0).supportedLanguageTag() == locale.toLanguageTag()) {
            return context
        }

        val configuration = Configuration(currentConfiguration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }

    private fun Locale.supportedLanguageTag(): String {
        val tag = toLanguageTag()
        return if (tag.contains("-") && tag != LocaleType.pt_br.tag) {
            tag.substringBefore("-")
        } else {
            tag
        }
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(LocaleHelper::class.java.name, Context.MODE_PRIVATE)
    }

    private fun persist(context: Context, locale: Locale?) {
        if (locale == null) return
        getPreferences(context)
            .edit()
            .putString(SELECTED_LANGUAGE, locale.toLanguageTag())
            .apply()
    }

}

enum class LocaleType(val tag: String) {
    de("de"),
    en("en"),
    es("es"),
    pt_br("pt-BR"),
    pt("pt"),
    fa("fa"),
    fr("fr"),
    ko("ko"),
    ru("ru"),
    uk("uk"),
    tr("tr"),
    zh("zh");
}
