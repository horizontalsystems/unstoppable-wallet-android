package io.horizontalsystems.core.modules.chart

import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class ChartNumberFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatNumberShort(value, 2)
    }

}
