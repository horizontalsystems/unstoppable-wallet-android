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
        val currencies = currencyManager.currencies.toMutableList()
        val populars = mutableListOf<Currency>()

        popularCurrencyCodes.forEach { code ->
            populars.add(currencies.removeAt(currencies.indexOfFirst { it.code == code }))
        }

        popularCurrencies = populars
        otherCurrencies = currencies
    }
}
