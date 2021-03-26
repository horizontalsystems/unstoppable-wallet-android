package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class RateFormatter(private val currency: Currency) : Chart.RateFormatter {
    override fun format(value: BigDecimal): String? {
        return App.numberFormatter.formatFiat(value, currency.symbol, 0, 2)
    }
}
