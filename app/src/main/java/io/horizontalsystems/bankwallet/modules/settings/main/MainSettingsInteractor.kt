package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.reactivex.disposables.CompositeDisposable

class MainSettingsInteractor(
        private val localStorage: ILocalStorage,
        languageManager: ILanguageManager,
        systemInfoManager: ISystemInfoManager,
        private val currencyManager: ICurrencyManager) : MainSettingsModule.IMainSettingsInteractor {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var delegate: MainSettingsModule.IMainSettingsInteractorDelegate? = null

    init {
        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            delegate?.didUpdateBaseCurrency(currencyManager.baseCurrency.code)
        })
    }

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
