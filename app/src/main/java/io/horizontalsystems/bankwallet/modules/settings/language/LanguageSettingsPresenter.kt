package io.horizontalsystems.bankwallet.modules.settings.language

class LanguageSettingsPresenter(
        private val router: LanguageSettingsModule.ILanguageSettingsRouter,
        private val interactor: LanguageSettingsModule.ILanguageSettingsInteractor)
    : LanguageSettingsModule.ILanguageSettingsViewDelegate {

    var view: LanguageSettingsModule.ILanguageSettingsView? = null

    private val availableLanguages = interactor.availableLanguages

    override fun viewDidLoad() {
        val currentLanguage = interactor.currentLanguage
        val items = availableLanguages.map { code ->
            LanguageViewItem(code, interactor.getName(code), interactor.getNativeName(code), currentLanguage == code)
        }

        view?.show(items)
    }

    override fun didSelect(position: Int) {
        interactor.setCurrentLanguage(availableLanguages[position])
        router.reloadAppInterface()
    }

}
