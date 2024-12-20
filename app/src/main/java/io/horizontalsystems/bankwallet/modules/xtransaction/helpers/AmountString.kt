package io.horizontalsystems.bankwallet.modules.xtransaction.helpers

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

@Composable
fun coinAmountString(value: BigDecimal?, coinCode: String, sign: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return sign + App.numberFormatter.formatCoinFull(value, coinCode, 8)
}

@Composable
fun fiatAmountString(value: BigDecimal?, fiatSymbol: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return App.numberFormatter.formatFiatFull(value, fiatSymbol)
}