package com.quantum.wallet.bankwallet.modules.chart

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Currency
import java.math.BigDecimal

class ChartCurrencyValueFormatterShortened : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatFiatShort(value, currency.symbol, 2)
    }

}
