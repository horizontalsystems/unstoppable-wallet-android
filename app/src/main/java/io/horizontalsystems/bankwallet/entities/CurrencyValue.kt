package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class CurrencyValue(val currency: Currency, val value: BigDecimal) : Parcelable{
    fun getFormatted(): String {
        return  App.numberFormatter.formatFiat(value, currency.symbol, 2, 2)
    }
}
