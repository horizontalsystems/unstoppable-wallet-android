package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.core.SingleLiveEvent

class MainSettingsRouter : MainSettingsModule.IMainSettingsRouter {

    val showSecuritySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showManageCoinsLiveEvent = SingleLiveEvent<Unit>()
    val showExperimentalFeaturesLiveEvent = SingleLiveEvent<Unit>()
    val showBaseCurrencySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showLanguageSettingsLiveEvent = SingleLiveEvent<Unit>()
    val showAboutLiveEvent = SingleLiveEvent<Unit>()
    val showNotificationsLiveEvent = SingleLiveEvent<Unit>()
    val showReportProblemLiveEvent = SingleLiveEvent<Unit>()
    val openLinkLiveEvent = SingleLiveEvent<String>()
    val shareAppLiveEvent = SingleLiveEvent<String>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()

    override fun showSecuritySettings() {
        showSecuritySettingsLiveEvent.call()
    }

    override fun showManageCoins() {
        showManageCoinsLiveEvent.call()
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

    override fun showAbout() {
        showAboutLiveEvent.call()
    }

    override fun openLink(url: String) {
        openLinkLiveEvent.postValue(url)
    }

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }

    override fun showReportProblem() {
        showReportProblemLiveEvent.call()
    }

    override fun showShareApp(appWebPageLink: String) {
        shareAppLiveEvent.postValue(appWebPageLink)
    }

    override fun showNotifications() {
        showNotificationsLiveEvent.call()
    }
}
