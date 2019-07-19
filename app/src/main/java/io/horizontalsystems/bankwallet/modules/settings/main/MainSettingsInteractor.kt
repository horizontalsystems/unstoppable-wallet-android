package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.core.*
import io.reactivex.disposables.CompositeDisposable

class MainSettingsInteractor(
        private val localStorage: ILocalStorage,
        private val backupManager: IBackupManager,
        languageManager: ILanguageManager,
        systemInfoManager: ISystemInfoManager,
        private val currencyManager: ICurrencyManager) : MainSettingsModule.IMainSettingsInteractor {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var delegate: MainSettingsModule.IMainSettingsInteractorDelegate? = null

    init {
        disposables.add(backupManager.nonBackedUpCountFlowable.subscribe {
            delegate?.didUpdateNonBackedUp(it)
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            delegate?.didUpdateBaseCurrency(currencyManager.baseCurrency.code)
        })
    }

    override val nonBackedUpCount: Int
        get() = backupManager.nonBackedUpCount

    override var currentLanguage: String = languageManager.currentLanguage.displayLanguage

    override val baseCurrency: String
        get() = currencyManager.baseCurrency.code

    override var appVersion: String = systemInfoManager.appVersion

    override fun getLightMode(): Boolean {
        return localStorage.isLightModeOn
    }

    override fun setLightMode(lightMode: Boolean) {
        localStorage.isLightModeOn = lightMode
        delegate?.didUpdateLightMode()
    }

    override fun clear() {
        disposables.clear()
    }

}
