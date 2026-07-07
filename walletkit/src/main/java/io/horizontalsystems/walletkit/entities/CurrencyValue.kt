package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.walletkit.core.App
import java.math.BigDecimal

data class CurrencyValue(val currency: Currency, val value: BigDecimal) {
    fun getFormattedFull(): String {
        return App.numberFormatter.formatFiatFull(value, currency.symbol)
    }

    fun getFormattedShort(): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }
}
