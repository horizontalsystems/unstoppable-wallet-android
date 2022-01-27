package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class ChartNumberFormatterShortened : ChartModule.ChartNumberFormatter {
    override fun formatValue(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatCurrencyValueAsShortened(currencyValue)
    }
}
