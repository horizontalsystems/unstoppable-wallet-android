package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.core.entities.Currency

object MainSettingsModule {

    interface IMainSettingsView {
        fun setBackedUp(backedUp: Boolean)
        fun setBaseCurrency(currency: String)
        fun setLanguage(language: String)
        fun setThemeName(themeName: Int)
        fun setAppVersion(appVersion: String)
        fun setTermsAccepted(termsAccepted: Boolean)
        fun setPinIsSet(pinSet: Boolean)
        fun setWalletConnectSessionCount(count: String?)
        fun setLaunchScreen(screen: LaunchPage)
    }

    interface IMainSettingsViewDelegate {
        fun viewDidLoad()
        fun didTapSecurity()
        fun didTapBaseCurrency()
        fun didTapLanguage()
        fun didTapTheme()
        fun didTapAboutApp()
        fun didTapCompanyLogo()
        fun didTapExperimentalFeatures()
        fun didTapManageKeys()
        fun didTapWalletConnect()
        fun didTapFaq()
        fun didTapAcademy()
        fun setAppRelaunchingFromSettings()
    }

    interface IMainSettingsInteractor {
        val themeName: Int
        val companyWebPageLink: String
        val appWebPageLink: String
        val allBackedUp: Boolean
        val walletConnectSessionCount: Int
        val currentLanguageDisplayName: String
        val baseCurrency: Currency
        val appVersion: String
        val termsAccepted: Boolean
        val isPinSet: Boolean
        val launchScreen: LaunchPage

        fun clear()
        fun setAppRelaunchingFromSettings()
    }

    interface IMainSettingsInteractorDelegate {
        fun didUpdateAllBackedUp(allBackedUp: Boolean)
        fun didUpdateBaseCurrency()
        fun didUpdateTermsAccepted(allAccepted: Boolean)
        fun didUpdatePinSet()
        fun didUpdateWalletConnectSessionCount(count: Int)
    }

    interface IMainSettingsRouter {
        fun showSecuritySettings()
        fun showBaseCurrencySettings()
        fun showLanguageSettings()
        fun showAboutApp()
        fun openLink(url: String)
        fun showExperimentalFeatures()
        fun showManageKeys()
        fun openWalletConnect()
        fun openFaq()
        fun openAcademy()
        fun showThemeSwitcher()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = MainSettingsView()
            val router = MainSettingsRouter()
            val interactor = MainSettingsInteractor(
                    localStorage = App.localStorage,
                    backupManager = App.backupManager,
                    languageManager = App.languageManager,
                    systemInfoManager = App.systemInfoManager,
                    currencyManager = App.currencyManager,
                    appConfigProvider = App.appConfigProvider,
                    termsManager = App.termsManager,
                    pinComponent = App.pinComponent,
                    walletConnectSessionManager = App.walletConnectSessionManager
            )
            val presenter = MainSettingsPresenter(view, router, interactor)
            interactor.delegate = presenter

            return presenter as T
        }
    }

}
