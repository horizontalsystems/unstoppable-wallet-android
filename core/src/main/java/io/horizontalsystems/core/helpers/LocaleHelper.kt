package io.horizontalsystems.core.helpers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

// https://github.com/zeugma-solutions/locale-helper-android

object LocaleHelper {

    val fallbackLocale: Locale = Locale.ENGLISH

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
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
        val systemLocale = context.resources.configuration.locales.get(0)
        var tag = systemLocale.toLanguageTag()

        //App language tags are in the format "en", except "pt-BR"
        if (tag.contains("-") && tag != LocaleType.pt_br.tag) {
            tag = tag.split("-")[0]
        }

        //use system locale if it is supported by app, else use fallback locale
        if (LocaleType.values().map { it.tag }.contains(tag)) {
            val localeFromSupportedTag = Locale.forLanguageTag(tag)
            setLocale(context, localeFromSupportedTag)
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

        val configuration = Configuration()
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
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
    fa("fa"),
    fr("fr"),
    ko("ko"),
    ru("ru"),
    tr("tr"),
    zh("zh");
}