package bitcoin.wallet.modules.settings.language

import bitcoin.wallet.core.ILanguageManager

class LanguageSettingsInteractor(private val languageManager: ILanguageManager): LanguageSettingsModule.ILanguageSettingsInteractor {

    var delegate: LanguageSettingsModule.ILanguageSettingsInteractorDelegate? = null

    override var items: List<LanguageItem> = listOf()
        get() {
            val currentLanguage = languageManager.currentLanguage
            return languageManager.availableLanguages.map { locale ->
                LanguageItem(locale, currentLanguage.language == locale.language)
            }
        }

    override fun setCurrentLanguage(item: LanguageItem) {
        languageManager.currentLanguage = item.locale
        delegate?.didSetCurrentLanguage()
    }
}
