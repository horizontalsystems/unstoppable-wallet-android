package com.quantum.wallet.bankwallet.modules.market.earn.vault

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.modules.chart.ChartModule
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
