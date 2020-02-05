package io.horizontalsystems.languageswitcher

import io.horizontalsystems.core.ILanguageConfigProvider
import io.horizontalsystems.core.ILanguageManager

class LanguageSwitcherInteractor(
        private val languageManager: ILanguageManager,
        private val appConfigProvider: ILanguageConfigProvider)
    : LanguageSwitcherModule.IInteractor {

    override var currentLanguage: String
        get() = languageManager.currentLanguage
        set(value) {
            languageManager.currentLanguage = value
        }

    override val availableLanguages: List<String>
        get() = appConfigProvider.localizations

    override fun getName(language: String): String {
        return languageManager.getName(language)
    }

    override fun getNativeName(language: String): String {
        return languageManager.getNativeName(language)
    }
}
