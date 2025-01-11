package io.horizontalsystems.core

import io.horizontalsystems.core.entities.Currency
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.SharedFlow

interface CurrencyManager {
    val baseCurrencyUpdatedFlow: SharedFlow<Unit>
    var baseCurrency: Currency
    val currencies: List<Currency>
    val baseCurrencyUpdatedSignal: PublishSubject<Unit>
    fun setBaseCurrencyCode(baseCurrencyCode: String)
}