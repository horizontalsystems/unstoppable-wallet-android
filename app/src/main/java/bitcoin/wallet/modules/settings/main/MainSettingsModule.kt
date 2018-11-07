package bitcoin.wallet.modules.settings.main

import bitcoin.wallet.core.App

object MainSettingsModule {

    interface IMainSettingsView {
        fun setTitle(title: Int)
        fun setBackedUp(backedUp: Boolean)
        fun setBaseCurrency(currency: String)
        fun setLanguage(language: String)
        fun setLightMode(lightMode: Boolean)
        fun setAppVersion(appVersion: String)
        fun setTabItemBadge(count: Int)
    }

    interface IMainSettingsViewDelegate {
        fun viewDidLoad()
        fun didTapSecurity()
        fun didTapBaseCurrency()
        fun didTapLanguage()
        fun didSwitchLightMode(lightMode: Boolean)
        fun didTapAbout()
        fun didTapAppLink()
    }

    interface IMainSettingsInteractor {
        var isBackedUp: Boolean
        var currentLanguage: String
        val baseCurrency: String
        var appVersion: String
        fun getLightMode(): Boolean
        fun setLightMode(lightMode: Boolean)
    }

    interface IMainSettingsInteractorDelegate {
        fun didBackup()
        fun didUpdateBaseCurrency(baseCurrency: String)
        fun didUpdateLightMode()
    }

    interface IMainSettingsRouter{
        fun showSecuritySettings()
        fun showBaseCurrencySettings()
        fun showLanguageSettings()
        fun showAbout()
        fun openAppLink()
        fun reloadAppInterface()
    }

    fun init(view: MainSettingsViewModel, router: IMainSettingsRouter) {
        val interactor = MainSettingsInteractor(
                localStorage = App.localStorage,
                wordsManager = App.wordsManager,
                languageManager = App.languageManager,
                systemInfoManager = App.systemInfoManager,
                currencyManager = App.currencyManager
        )

        val presenter = MainSettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
