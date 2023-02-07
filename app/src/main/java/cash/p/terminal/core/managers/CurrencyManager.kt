package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Currency
import io.reactivex.subjects.PublishSubject

class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: AppConfigProvider) {

    var baseCurrency = getInitialCurrency()
        set(value) {
            field = value

            localStorage.baseCurrencyCode = value.code
            baseCurrencyUpdatedSignal.onNext(Unit)
        }

    private fun getInitialCurrency(): Currency {
        return localStorage.baseCurrencyCode?.let { code ->
            appConfigProvider.currencies.find { it.code == code }
        } ?: appConfigProvider.currencies.first { it.code == "USD" }
    }

    val currencies: List<Currency> = appConfigProvider.currencies

    val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
}
