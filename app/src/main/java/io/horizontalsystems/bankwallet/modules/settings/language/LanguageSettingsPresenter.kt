package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.ViewModel

class LanguageSettingsPresenter(
        val view: LanguageSettingsModule.ILanguageSettingsView,
        val router: LanguageSettingsModule.ILanguageSettingsRouter,
        private val interactor: LanguageSettingsModule.ILanguageSettingsInteractor
) : ViewModel(), LanguageSettingsModule.ILanguageSettingsViewDelegate {

    private val languages = interactor.availableLanguages

    override fun viewDidLoad() {
        val currentLanguage = interactor.currentLanguage
        val items = languages.map { language ->
            LanguageViewItem(language, interactor.getName(language), interactor.getNativeName(language), currentLanguage == language)
        }

        view.show(items)
    }

    override fun didSelect(position: Int) {
        interactor.currentLanguage = languages[position]
        router.reloadAppInterface()
    }

}
