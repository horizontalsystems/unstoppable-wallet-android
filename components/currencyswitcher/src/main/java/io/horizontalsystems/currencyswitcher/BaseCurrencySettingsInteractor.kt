package io.horizontalsystems.currencyswitcher

import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency

class BaseCurrencySettingsInteractor(private val currencyManager: ICurrencyManager) : BaseCurrencySettingsModule.IInteractor {

    override val currencies: List<Currency>
        get() = currencyManager.currencies

    override var baseCurrency: Currency
        get() = currencyManager.baseCurrency
        set(value) {
            currencyManager.baseCurrency = value
        }
}
