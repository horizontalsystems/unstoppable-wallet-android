package io.horizontalsystems.core.modules.market.earn.vault

import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.modules.chart.ChartModule
import java.math.BigDecimal

class VaultChartFormatter : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.format(value, 0, 2, "APY ", "%")
    }

    override fun formatMinMaxValue(
        currency: Currency,
        value: BigDecimal
    ): String {
        return App.numberFormatter.format(value, 0, 2, "", "%")
    }
}
