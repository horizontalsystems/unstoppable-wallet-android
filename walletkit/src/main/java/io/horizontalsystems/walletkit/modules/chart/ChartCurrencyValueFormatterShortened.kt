package io.horizontalsystems.walletkit.modules.chart

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }

}
