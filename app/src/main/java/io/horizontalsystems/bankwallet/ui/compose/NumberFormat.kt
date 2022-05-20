package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BigDecimalRounded
import io.horizontalsystems.bankwallet.core.managers.NumberSuffix

@Composable
fun formatNumber(
    number: BigDecimalRounded,
    coinCode: String? = null,
): String {
    val numberStr = App.numberFormatter.format(number.value, 0, Int.MAX_VALUE)
    var res = when (number) {
        is BigDecimalRounded.Large -> {
            val suffixStr = when (number.suffix) {
                NumberSuffix.Blank -> ""
                NumberSuffix.Thousand -> stringResource(id = R.string.CoinPage_MarketCap_Thousand)
                NumberSuffix.Million -> stringResource(id = R.string.CoinPage_MarketCap_Million)
                NumberSuffix.Billion -> stringResource(id = R.string.CoinPage_MarketCap_Billion)
                NumberSuffix.Trillion -> stringResource(id = R.string.CoinPage_MarketCap_Trillion)
            }
            numberStr + suffixStr
        }
        is BigDecimalRounded.LessThen -> {
            "< $numberStr"
        }
        is BigDecimalRounded.Regular -> {
            numberStr
        }
    }

    coinCode?.let {
        res = "$res $it"
    }

    return res
}
