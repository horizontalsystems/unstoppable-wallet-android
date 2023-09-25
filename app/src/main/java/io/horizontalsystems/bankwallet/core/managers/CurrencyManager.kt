package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Currency
import io.reactivex.subjects.PublishSubject

class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: AppConfigProvider) {

    var baseCurrency = getInitialCurrency()
        set(value) {
            field = value

            localStorage.baseCurrencyCode = value.code
            baseCurrencyUpdatedSignal.onNext(Unit)
        }

    private val defaultCurrency: Currency
        get() = appConfigProvider.currencies.first { it.code == "USD" }

    private fun getInitialCurrency(): Currency {
        return localStorage.baseCurrencyCode?.let { code ->
            appConfigProvider.currencies.find { it.code == code }
        } ?: defaultCurrency
    }

    val currencies: List<Currency> = appConfigProvider.currencies

    val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()

    fun setBaseCurrencyCode(baseCurrencyCode: String) {
        val newCurrency = appConfigProvider.currencies.find { it.code == baseCurrencyCode }
        baseCurrency = newCurrency ?: defaultCurrency
    }
}
