package io.horizontalsystems.walletkit.modules.chart

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

class ChartCoinValueFormatterShortened(private val fullCoin: FullCoin) : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatCoinShort(value, fullCoin.coin.code, 8)
    }

}
