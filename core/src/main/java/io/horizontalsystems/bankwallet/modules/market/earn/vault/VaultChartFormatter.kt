package io.horizontalsystems.bankwallet.modules.market.earn.vault

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
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
