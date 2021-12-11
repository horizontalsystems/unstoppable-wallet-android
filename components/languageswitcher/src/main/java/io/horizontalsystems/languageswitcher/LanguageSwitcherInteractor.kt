package io.horizontalsystems.languageswitcher

import io.horizontalsystems.core.ILanguageManager

class LanguageSwitcherInteractor(private val languageManager: ILanguageManager) :
    LanguageSwitcherModule.IInteractor {

    private val localizations = "de,en,es,fa,fr,ko,ru,tr,zh"

    override var currentLanguage: String
        get() = languageManager.currentLanguage
        set(value) {
            languageManager.currentLanguage = value
        }

    override val availableLanguages: List<String>
        get() {
            return localizations.split(",")
        }

    override fun getName(language: String): String {
        return languageManager.getName(language)
    }

    override fun getNativeName(language: String): String {
        return languageManager.getNativeName(language)
    }
}
