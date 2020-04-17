package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.IMainSettingsInteractor
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.IMainSettingsInteractorDelegate
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.IMainSettingsRouter
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.IMainSettingsView
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.IMainSettingsViewDelegate

class MainSettingsPresenter(
        val view: IMainSettingsView,
        val router: IMainSettingsRouter,
        private val interactor: IMainSettingsInteractor)
    : ViewModel(), IMainSettingsViewDelegate, IMainSettingsInteractorDelegate {

    private val helper = MainSettingsHelper()

    override fun viewDidLoad() {
        view.setBackedUp(interactor.allBackedUp)
        view.setBaseCurrency(helper.displayName(interactor.baseCurrency))
        view.setLanguage(interactor.currentLanguageDisplayName)
        view.setLightMode(interactor.lightMode)
        view.setAppVersion(interactor.appVersion)
    }

    override fun didTapManageKeys() {
        router.showManageKeys()
    }

    override fun didTapSecurity() {
        router.showSecuritySettings()
    }

    override fun didTapExperimentalFeatures() {
        router.showExperimentalFeatures()
    }

    override fun didTapBaseCurrency() {
        router.showBaseCurrencySettings()
    }

    override fun didTapLanguage() {
        router.showLanguageSettings()
    }

    override fun didSwitchLightMode(lightMode: Boolean) {
        interactor.lightMode = lightMode
        router.reloadAppInterface()
    }

    override fun didTapAbout() {
        router.showAbout()
    }

    override fun didTapCompanyLogo() {
        router.openLink(interactor.companyWebPageLink)
    }

    override fun didTapReportProblem() {
        router.showReportProblem()
    }

    override fun didTapTellFriends() {
        router.showShareApp(interactor.appWebPageLink)
    }

    override fun didTapNotifications() {
        router.showNotifications()
    }

    // IMainSettingsInteractorDelegate

    override fun didUpdateAllBackedUp(allBackedUp: Boolean) {
        view.setBackedUp(allBackedUp)
    }

    override fun didUpdateBaseCurrency() {
        view.setBaseCurrency(helper.displayName(interactor.baseCurrency))
    }

    // ViewModel

    override fun onCleared() {
        interactor.clear()
    }

}
