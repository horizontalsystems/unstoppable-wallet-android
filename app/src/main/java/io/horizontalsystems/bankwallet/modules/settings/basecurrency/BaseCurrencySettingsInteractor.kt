package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency

class BaseCurrencySettingsInteractor(
        private val currencyManager: ICurrencyManager): BaseCurrencySettingsModule.IInteractor {

    override val currencies: List<Currency>
        get() = currencyManager.currencies

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override fun setBaseCurrency(currency: Currency) {
        currencyManager.setBaseCurrency(currency)
    }
}
