package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.IThemeStorage
import io.reactivex.disposables.CompositeDisposable

class MainSettingsInteractor(
        private val themeStorage: IThemeStorage,
        private val backupManager: IBackupManager,
        private val languageManager: ILanguageManager,
        private val systemInfoManager: ISystemInfoManager,
        private val currencyManager: ICurrencyManager,
        private val appConfigProvider: IAppConfigProvider)
    : MainSettingsModule.IMainSettingsInteractor {

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
        get() = themeStorage.isLightModeOn
        set(value) {
            themeStorage.isLightModeOn = value
        }

    override val appVersion: String
        get() = systemInfoManager.appVersion

    override fun clear() {
        disposables.clear()
    }

}
