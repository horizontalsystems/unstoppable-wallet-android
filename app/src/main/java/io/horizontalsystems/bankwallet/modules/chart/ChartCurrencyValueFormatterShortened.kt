package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        val currencyValue = CurrencyValue(currency, value)
        return App.numberFormatter.formatCurrencyValueAsShortened(currencyValue)
    }

}
