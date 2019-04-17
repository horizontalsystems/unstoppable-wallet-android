package io.horizontalsystems.bankwallet.modules.settings.language

class LanguageSettingsPresenter(
        private val router: LanguageSettingsModule.ILanguageSettingsRouter,
        private val interactor: LanguageSettingsModule.ILanguageSettingsInteractor)
    : LanguageSettingsModule.ILanguageSettingsViewDelegate, LanguageSettingsModule.ILanguageSettingsInteractorDelegate {

    var view: LanguageSettingsModule.ILanguageSettingsView? = null

    override fun didSetCurrentLanguage() {
        router.reloadAppInterface()
    }

    override fun viewDidLoad() {
        view?.show(items = interactor.items)
    }

    override fun didSelect(item: LanguageItem) {
        interactor.setCurrentLanguage(item)
    }

}
