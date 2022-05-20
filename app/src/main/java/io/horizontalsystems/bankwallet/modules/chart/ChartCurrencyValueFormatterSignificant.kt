package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterSignificant : ChartModule.ChartNumberFormatter {
    override fun formatValue(currency: Currency, value: BigDecimal): String {
        val significantDecimal = App.numberFormatter.getSignificantDecimalFiat(value)
        return App.numberFormatter.formatFiat(
            value,
            currency.symbol,
            2,
            significantDecimal
        )
    }
}
