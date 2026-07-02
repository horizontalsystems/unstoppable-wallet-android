package io.horizontalsystems.walletkit.modules.market.earn.vault

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.chart.ChartModule
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
