package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.ChartView
import java.math.BigDecimal

class RateFormatter(private val currency: Currency) : ChartView.RateFormatter {
    override fun format(value: BigDecimal, maxFraction: Int?): String? {
        return App.numberFormatter.formatForRates(CurrencyValue(currency, value), maxFraction = maxFraction)
    }
}
