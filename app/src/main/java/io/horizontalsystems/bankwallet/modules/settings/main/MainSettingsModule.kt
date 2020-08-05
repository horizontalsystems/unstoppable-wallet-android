package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency

object MainSettingsModule {

    interface IMainSettingsView {
        fun setBackedUp(backedUp: Boolean)
        fun setBaseCurrency(currency: String)
        fun setLanguage(language: String)
        fun setLightMode(lightMode: Boolean)
        fun setAppVersion(appVersion: String)
        fun setTermsAccepted(termsAccepted: Boolean)
        fun setPinIsSet(pinSet: Boolean)
    }

    interface IMainSettingsViewDelegate {
        fun viewDidLoad()
        fun onViewResume()
        fun didTapSecurity()
        fun didTapBaseCurrency()
        fun didTapLanguage()
        fun didSwitchLightMode(lightMode: Boolean)
        fun didTapAbout()
        fun didTapCompanyLogo()
        fun didTapReportProblem()
        fun didTapTellFriends()
        fun didTapNotifications()
        fun didTapExperimentalFeatures()
        fun didTapManageKeys()
        fun didTapAppStatus()
    }

    interface IMainSettingsInteractor {
        val companyWebPageLink: String
        val appWebPageLink: String
        val allBackedUp: Boolean
        val currentLanguageDisplayName: String
        val baseCurrency: Currency
        val appVersion: String
        var lightMode: Boolean
        val termsAccepted: Boolean
        val isPinSet: Boolean

        fun clear()
    }

    interface IMainSettingsInteractorDelegate {
        fun didUpdateAllBackedUp(allBackedUp: Boolean)
        fun didUpdateBaseCurrency()
        fun didUpdateTermsAccepted(allAccepted: Boolean)
    }

    interface IMainSettingsRouter {
        fun showSecuritySettings()
        fun showBaseCurrencySettings()
        fun showLanguageSettings()
        fun showAbout()
        fun openLink(url: String)
        fun reloadAppInterface()
        fun showReportProblem()
        fun showShareApp(appWebPageLink: String)
        fun showNotifications()
        fun showExperimentalFeatures()
        fun showManageKeys()
        fun openAppStatus()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = MainSettingsView()
            val router = MainSettingsRouter()
            val interactor = MainSettingsInteractor(
                    themeStorage = App.themeStorage,
                    backupManager = App.backupManager,
                    languageManager = App.languageManager,
                    systemInfoManager = App.systemInfoManager,
                    currencyManager = App.currencyManager,
                    appConfigProvider = App.appConfigProvider,
                    termsManager = App.termsManager,
                    pinComponent = App.pinComponent
            )
            val presenter = MainSettingsPresenter(view, router, interactor)
            interactor.delegate = presenter

            return presenter as T
        }
    }

}
