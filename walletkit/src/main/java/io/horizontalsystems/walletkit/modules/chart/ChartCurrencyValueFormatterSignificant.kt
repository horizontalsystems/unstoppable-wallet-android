package io.horizontalsystems.walletkit.modules.chart

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterSignificant : ChartModule.ChartNumberFormatter {
    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatFiatFull(value, currency.symbol)
    }
}
