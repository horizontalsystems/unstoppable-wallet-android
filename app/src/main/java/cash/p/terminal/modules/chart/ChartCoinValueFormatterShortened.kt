package cash.p.terminal.modules.chart

import cash.p.terminal.core.App
import cash.p.terminal.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

class ChartCoinValueFormatterShortened(private val fullCoin: FullCoin) : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatCoinShort(value, fullCoin.coin.code, 8)
    }

}
