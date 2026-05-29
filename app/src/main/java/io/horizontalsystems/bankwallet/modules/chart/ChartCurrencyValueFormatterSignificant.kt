package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterSignificant : ChartModule.ChartNumberFormatter {
    override fun formatValue(currency: Currency, value: BigDecimal, numberFormatter: IAppNumberFormatter): String {
        return numberFormatter.formatFiatFull(value, currency.symbol)
    }
}
