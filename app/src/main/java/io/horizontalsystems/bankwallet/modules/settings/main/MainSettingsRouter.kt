package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.core.SingleLiveEvent

class MainSettingsRouter : MainSettingsModule.IMainSettingsRouter {

    val showSecuritySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showExperimentalFeaturesLiveEvent = SingleLiveEvent<Unit>()
    val showBaseCurrencySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showLanguageSettingsLiveEvent = SingleLiveEvent<Unit>()
    val showThemeSwitcherLiveEvent = SingleLiveEvent<Unit>()
    val showAboutLiveEvent = SingleLiveEvent<Unit>()
    val openLinkLiveEvent = SingleLiveEvent<String>()
    val showManageKeysLiveEvent = SingleLiveEvent<Unit>()
    val openWalletConnectLiveEvent = SingleLiveEvent<Unit>()
    val openFaqLiveEvent = SingleLiveEvent<Unit>()
    val openAcademyLiveEvent = SingleLiveEvent<Unit>()

    override fun showManageKeys() {
        showManageKeysLiveEvent.call()
    }

    override fun showSecuritySettings() {
        showSecuritySettingsLiveEvent.call()
    }

    override fun showExperimentalFeatures() {
        showExperimentalFeaturesLiveEvent.call()
    }

    override fun showBaseCurrencySettings() {
        showBaseCurrencySettingsLiveEvent.call()
    }

    override fun showLanguageSettings() {
        showLanguageSettingsLiveEvent.call()
    }

    override fun showAboutApp() {
        showAboutLiveEvent.call()
    }

    override fun openLink(url: String) {
        openLinkLiveEvent.postValue(url)
    }

    override fun openWalletConnect() {
        openWalletConnectLiveEvent.call()
    }

    override fun openFaq() {
        openFaqLiveEvent.call()
    }

    override fun openAcademy() {
        openAcademyLiveEvent.call()
    }

    override fun showThemeSwitcher() {
        showThemeSwitcherLiveEvent.call()
    }
}
