package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class ChartNumberFormatterSignificant : ChartModule.ChartNumberFormatter {
    override fun formatValue(currencyValue: CurrencyValue): String {
        val significantDecimal = App.numberFormatter.getSignificantDecimalFiat(currencyValue.value)
        return App.numberFormatter.formatFiat(
            currencyValue.value,
            currencyValue.currency.symbol,
            2,
            significantDecimal
        )
    }
}
