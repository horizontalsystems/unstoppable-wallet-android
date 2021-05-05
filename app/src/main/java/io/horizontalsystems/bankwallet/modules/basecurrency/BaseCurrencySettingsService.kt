package io.horizontalsystems.bankwallet.modules.basecurrency

import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency

class BaseCurrencySettingsService(private val currencyManager: ICurrencyManager) {
    var baseCurrency: Currency
        get() = currencyManager.baseCurrency
        set(value) {
            currencyManager.baseCurrency = value
        }

    private val popularCurrencyCodes = listOf("USD", "EUR", "GBP", "JPY")

    val popularCurrencies: List<Currency>
    val otherCurrencies: List<Currency>

    init {
        val (popularCurrencies, otherCurrencies) = currencyManager.currencies.partition {
            popularCurrencyCodes.contains(it.code)
        }

        this.popularCurrencies = popularCurrencies
        this.otherCurrencies = otherCurrencies
    }
}
