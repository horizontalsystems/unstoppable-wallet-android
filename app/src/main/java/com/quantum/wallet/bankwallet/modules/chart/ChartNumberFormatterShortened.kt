package com.quantum.wallet.bankwallet.modules.chart

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Currency
import java.math.BigDecimal

class ChartNumberFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatNumberShort(value, 2)
    }

}
