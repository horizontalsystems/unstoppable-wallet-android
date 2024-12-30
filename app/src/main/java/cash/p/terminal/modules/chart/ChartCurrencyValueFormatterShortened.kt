package cash.p.terminal.modules.chart

import cash.p.terminal.core.App
import io.horizontalsystems.chartview.chart.ChartModule
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }

}
