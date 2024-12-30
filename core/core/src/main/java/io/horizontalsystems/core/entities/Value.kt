package io.horizontalsystems.core.entities

import java.math.BigDecimal

sealed class Value {
    class Percent(val percent: BigDecimal) : Value()
    class Currency(val currencyValue: CurrencyValue) : Value()

    fun raw() = when (this) {
        is Currency -> currencyValue.value
        is Percent -> percent
    }
}