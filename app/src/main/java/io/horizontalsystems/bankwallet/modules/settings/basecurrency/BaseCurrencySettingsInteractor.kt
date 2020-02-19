package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import io.horizontalsystems.bankwallet.core.ICurrencyManager
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
