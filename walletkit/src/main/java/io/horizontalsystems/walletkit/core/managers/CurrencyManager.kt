package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.entities.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: IAppConfigProvider) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _baseCurrencyUpdatedFlow: MutableSharedFlow<Unit> = MutableSharedFlow()
    val baseCurrencyUpdatedFlow: SharedFlow<Unit> = _baseCurrencyUpdatedFlow.asSharedFlow()

    var baseCurrency = getInitialCurrency()
        set(value) {
            field = value

            localStorage.baseCurrencyCode = value.code
            scope.launch {
                _baseCurrencyUpdatedFlow.emit(Unit)
            }
        }

    private val defaultCurrency: Currency
        get() = appConfigProvider.currencies.first { it.code == "USD" }

    private fun getInitialCurrency(): Currency {
        return localStorage.baseCurrencyCode?.let { code ->
            appConfigProvider.currencies.find { it.code == code }
        } ?: defaultCurrency
    }

    val currencies: List<Currency> = appConfigProvider.currencies

    fun setBaseCurrencyCode(baseCurrencyCode: String) {
        val newCurrency = appConfigProvider.currencies.find { it.code == baseCurrencyCode }
        baseCurrency = newCurrency ?: defaultCurrency
    }
}
