package io.horizontalsystems.currencyswitcher

import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency

class CurrencySwitcherInteractor(private val currencyManager: ICurrencyManager) : CurrencySwitcherModule.IInteractor {

    override val currencies: List<Currency>
        get() = currencyManager.currencies

    override var baseCurrency: Currency
        get() = currencyManager.baseCurrency
        set(value) {
            currencyManager.baseCurrency = value
        }
}
