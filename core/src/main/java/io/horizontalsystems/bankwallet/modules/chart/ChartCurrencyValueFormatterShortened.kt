package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }

}
