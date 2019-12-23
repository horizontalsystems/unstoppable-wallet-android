package io.horizontalsystems.bankwallet.localehelper

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.*

// https://github.com/zeugma-solutions/locale-helper-android

object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val SELECTED_COUNTRY = "Locale.Helper.Selected.Country"
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

    private fun updateContextLocale(context: Context, locale: Locale): Context = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> updateResources(context, locale)
        else -> updateResourcesLegacy(context, locale)
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(LocaleHelper::class.java.name, Context.MODE_PRIVATE)
    }

    private fun persist(context: Context, locale: Locale?) {
        if (locale == null) return
        getPreferences(context)
            .edit()
            .putString(SELECTED_LANGUAGE, locale.language)
            .putString(SELECTED_COUNTRY, locale.country)
            .apply()
    }

    private fun load(context: Context): Locale {
        val preferences = getPreferences(context)
        val language = preferences.getString(SELECTED_LANGUAGE, Locale.getDefault().language)
        val country = preferences.getString(SELECTED_COUNTRY, Locale.getDefault().country)
        return Locale(language, country)
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        configuration.setLayoutDirection(locale)

        // we need to call deprecated updateConfiguration() to make Application context work with custom locale
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)

        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val resources = context.resources

        val configuration = resources.configuration
        configuration.locale = locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)

        return context
    }

}