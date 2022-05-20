package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class ChartNumberFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        val (shortValue, suffix) = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.format(shortValue, 0, 2) + " $suffix"
    }

}
