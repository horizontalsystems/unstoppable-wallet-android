package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.subjects.PublishSubject

class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: AppConfigProvider) : ICurrencyManager {

    override var baseCurrency = getInitialCurrency()
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

    override val currencies: List<Currency> = appConfigProvider.currencies

    override val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
}
