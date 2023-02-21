package io.horizontalsystems.core.helpers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.*

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
        val locale = load(context)
        return updateContextLocale(context, locale)
    }

    fun getLocale(context: Context): Locale {
        return load(context)
    }

    fun setLocale(context: Context, locale: Locale) {
        persist(context, locale)

        updateContextLocale(context, locale)
    }

    fun isRTL(locale: Locale): Boolean {
        return RTL.contains(locale.language)
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

    private fun load(context: Context): Locale {
        val preferences = getPreferences(context)
        val languageTag = preferences.getString(SELECTED_LANGUAGE, null) ?: fallbackLocale.toLanguageTag()
        return Locale.forLanguageTag(languageTag)
    }

}
