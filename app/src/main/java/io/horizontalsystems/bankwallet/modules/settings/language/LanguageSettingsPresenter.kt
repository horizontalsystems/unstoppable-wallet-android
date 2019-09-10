package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.ViewModel

class LanguageSettingsPresenter(
        val view: LanguageSettingsModule.ILanguageSettingsView,
        val router: LanguageSettingsModule.ILanguageSettingsRouter,
        private val interactor: LanguageSettingsModule.ILanguageSettingsInteractor
) : ViewModel(), LanguageSettingsModule.ILanguageSettingsViewDelegate {

    private val availableLanguages = interactor.availableLanguages

    override fun viewDidLoad() {
        val currentLanguage = interactor.currentLanguage
        val items = availableLanguages.map { code ->
            LanguageViewItem(code, interactor.getName(code), interactor.getNativeName(code), currentLanguage == code)
        }

        view.show(items)
    }

    override fun didSelect(position: Int) {
        interactor.setCurrentLanguage(availableLanguages[position])
        router.reloadAppInterface()
    }

}
