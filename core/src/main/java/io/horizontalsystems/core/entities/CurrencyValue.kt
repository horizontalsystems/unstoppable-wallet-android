package io.horizontalsystems.core.entities

import android.os.Parcelable
import io.horizontalsystems.core.core.App
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CurrencyValue(val currency: Currency, val value: BigDecimal) : Parcelable {
    fun getFormattedFull(): String {
        return App.numberFormatter.formatFiatFull(value, currency.symbol)
    }

    fun getFormattedShort(): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }
}
