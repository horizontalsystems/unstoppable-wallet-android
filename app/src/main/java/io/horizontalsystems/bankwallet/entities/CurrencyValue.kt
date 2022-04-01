package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CurrencyValue(val currency: Currency, val value: BigDecimal) : Parcelable {
    fun getFormatted(minimumFractionDigits: Int = 2, maximumFractionDigits: Int = 2): String {
        return App.numberFormatter.formatFiat(
            value,
            currency.symbol,
            minimumFractionDigits,
            maximumFractionDigits
        )
    }
}
