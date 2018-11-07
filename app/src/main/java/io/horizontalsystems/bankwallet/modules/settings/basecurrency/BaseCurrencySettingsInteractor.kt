package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency

class BaseCurrencySettingsInteractor(
        private val currencyManager: ICurrencyManager): BaseCurrencySettingsModule.IBaseCurrencySettingsInteractor {

    var delegate: BaseCurrencySettingsModule.IBaseCurrencySettingsInteractorDelegate? = null

    override val currencies: List<Currency>
        get() = currencyManager.currencies

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override fun setBaseCurrency(code: String) {
        currencyManager.setBaseCurrency(code= code)
        delegate?.didSetBaseCurrency()
    }
}
