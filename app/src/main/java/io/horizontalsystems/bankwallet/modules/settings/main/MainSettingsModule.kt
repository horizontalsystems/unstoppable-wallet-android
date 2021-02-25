package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trustwallet.walletconnect.models.WCPeerMeta
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
        fun setWalletConnectSessionCount(count: String?)
    }

    interface IMainSettingsViewDelegate {
        fun viewDidLoad()
        fun didTapSecurity()
        fun didTapBaseCurrency()
        fun didTapLanguage()
        fun didSwitchLightMode(lightMode: Boolean)
        fun didTapAboutApp()
        fun didTapCompanyLogo()
        fun didTapNotifications()
        fun didTapExperimentalFeatures()
        fun didTapManageKeys()
        fun didTapWalletConnect()
        fun didTapFaq()
        fun didTapAcademy()
        fun didTapTwitter()
        fun didTapTelegram()
        fun didTapReddit()
    }

    interface IMainSettingsInteractor {
        val companyWebPageLink: String
        val appWebPageLink: String
        val companyTwitterLink: String
        val companyTelegramLink: String
        val companyRedditLink: String
        val allBackedUp: Boolean
        val walletConnectSessionCount: Int
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
        fun didUpdatePinSet()
        fun didUpdateWalletConnectSessionCount(count: Int)
    }

    interface IMainSettingsRouter {
        fun showSecuritySettings()
        fun showBaseCurrencySettings()
        fun showLanguageSettings()
        fun showAboutApp()
        fun openLink(url: String)
        fun reloadAppInterface()
        fun showNotifications()
        fun showExperimentalFeatures()
        fun showManageKeys()
        fun openWalletConnect()
        fun openFaq()
        fun openAcademy()
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
                    pinComponent = App.pinComponent,
                    walletConnectSessionManager = App.walletConnectSessionManager
            )
            val presenter = MainSettingsPresenter(view, router, interactor)
            interactor.delegate = presenter

            return presenter as T
        }
    }

}
