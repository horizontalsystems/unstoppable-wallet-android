package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Currency
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: AppConfigProvider) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _baseCurrencyUpdatedFlow: MutableSharedFlow<Unit> = MutableSharedFlow()
    val baseCurrencyUpdatedFlow: SharedFlow<Unit> = _baseCurrencyUpdatedFlow.asSharedFlow()

    var baseCurrency = getInitialCurrency()
        set(value) {
            field = value

            localStorage.baseCurrencyCode = value.code
            baseCurrencyUpdatedSignal.onNext(Unit)
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

    val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()

    fun setBaseCurrencyCode(baseCurrencyCode: String) {
        val newCurrency = appConfigProvider.currencies.find { it.code == baseCurrencyCode }
        baseCurrency = newCurrency ?: defaultCurrency
    }
}
