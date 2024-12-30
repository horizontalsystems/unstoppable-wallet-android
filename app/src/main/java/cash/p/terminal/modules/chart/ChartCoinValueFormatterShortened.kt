package cash.p.terminal.modules.chart

import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.wallet.entities.FullCoin
import io.horizontalsystems.chartview.chart.ChartModule
import java.math.BigDecimal

class ChartCoinValueFormatterShortened(private val fullCoin: FullCoin) : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatCoinShort(value, fullCoin.coin.code, 8)
    }

}
