package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import com.trustwallet.walletconnect.models.WCPeerMeta
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
        view.setTermsAccepted(interactor.termsAccepted)
        view.setPinIsSet(interactor.isPinSet)
        setWalletConnectSessionCount(interactor.walletConnectSessionCount)
    }

    private fun setWalletConnectSessionCount(count: Int) {
        view.setWalletConnectSessionCount(if (count > 0) "$count" else null)
    }

    override fun didTapManageKeys() {
        router.showManageKeys()
    }

    override fun didTapWalletConnect() {
        router.openWalletConnect()
    }

    override fun didTapFaq() {
        router.openFaq()
    }

    override fun didTapAcademy() {
        router.openAcademy()
    }

    override fun didTapTwitter() {
        router.openLink(interactor.companyTwitterLink)
    }

    override fun didTapTelegram() {
        router.openLink(interactor.companyTelegramLink)
    }

    override fun didTapReddit() {
        router.openLink(interactor.companyRedditLink)
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

    override fun didTapAboutApp() {
        router.showAboutApp()
    }

    override fun didTapCompanyLogo() {
        router.openLink(interactor.companyWebPageLink)
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

    override fun didUpdateTermsAccepted(allAccepted: Boolean) {
        view.setTermsAccepted(allAccepted)
    }

    override fun didUpdatePinSet() {
        view.setPinIsSet(interactor.isPinSet)
    }

    override fun didUpdateWalletConnectSessionCount(count: Int) {
        setWalletConnectSessionCount(count)
    }

    // ViewModel

    override fun onCleared() {
        interactor.clear()
    }

}
