package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.reactivex.disposables.CompositeDisposable

class MainSettingsInteractor(
        private val localStorage: ILocalStorage,
        private val backupManager: IBackupManager,
        private val languageManager: ILanguageManager,
        private val systemInfoManager: ISystemInfoManager,
        private val currencyManager: ICurrencyManager,
        private val appConfigProvider: IAppConfigProvider) : MainSettingsModule.IMainSettingsInteractor {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var delegate: MainSettingsModule.IMainSettingsInteractorDelegate? = null

    init {
        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            delegate?.didUpdateAllBackedUp(it)
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            delegate?.didUpdateBaseCurrency()
        })
    }

    override val companyWebPageLink: String
        get() = appConfigProvider.companyWebPageLink

    override val appWebPageLink: String
        get() = appConfigProvider.appWebPageLink

    override val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    override val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override var lightMode: Boolean
        get() = localStorage.isLightModeOn
        set(value) {
            localStorage.isLightModeOn = value
        }

    override val appVersion: String
        get() = systemInfoManager.appVersion

    override fun clear() {
        disposables.clear()
    }

}
