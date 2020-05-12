package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class RateFormatter(private val currency: Currency) : Chart.RateFormatter {
    override fun format(value: BigDecimal): String? {
        return App.numberFormatter.formatForRates(CurrencyValue(currency, value), trimmable = true)
    }
}
