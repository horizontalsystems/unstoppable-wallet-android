package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

class ChartCoinValueFormatterShortened(private val fullCoin: FullCoin) : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal, numberFormatter: IAppNumberFormatter): String {
        return numberFormatter.formatCoinShort(value, fullCoin.coin.code, 8)
    }

}
