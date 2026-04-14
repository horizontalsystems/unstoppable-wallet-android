package com.quantum.wallet.bankwallet.modules.chart

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

class ChartCoinValueFormatterShortened(private val fullCoin: FullCoin) : ChartModule.ChartNumberFormatter {

    override fun formatValue(currency: Currency, value: BigDecimal): String {
        return App.numberFormatter.formatCoinShort(value, fullCoin.coin.code, 8)
    }

}
