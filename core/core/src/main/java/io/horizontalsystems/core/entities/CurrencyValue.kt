package io.horizontalsystems.core.entities

import android.os.Parcelable
import io.horizontalsystems.core.IAppNumberFormatter
import kotlinx.parcelize.Parcelize
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

@Parcelize
data class CurrencyValue(val currency: Currency, val value: BigDecimal) : Parcelable {
    private val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
    fun getFormattedFull(): String {
        return numberFormatter.formatFiatFull(value, currency.symbol)
    }

    fun getFormattedShort(): String {
        return numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }
}
