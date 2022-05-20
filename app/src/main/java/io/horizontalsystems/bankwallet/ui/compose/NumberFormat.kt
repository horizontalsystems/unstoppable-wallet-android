package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BigDecimalRounded
import io.horizontalsystems.bankwallet.core.managers.NumberSuffix
import io.horizontalsystems.bankwallet.entities.CoinValueRounded
import io.horizontalsystems.bankwallet.entities.CurrencyValueRounded

@Composable
fun formatNumberCoin(coinValueRounded: CoinValueRounded): String {
    val numberStr = App.numberFormatter.format(coinValueRounded.value.value, 0, Int.MAX_VALUE)
    val res = when (coinValueRounded.value) {
        is BigDecimalRounded.Large -> {
            val suffixStr = when (coinValueRounded.value.suffix) {
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

    return res + " ${coinValueRounded.platformCoin.coin.code}"
}

@Composable
fun formatNumber(number: BigDecimalRounded): String {
    val numberStr = App.numberFormatter.format(number.value, 0, Int.MAX_VALUE)
    return when (number) {
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
}

@Composable
fun formatNumberFiat(
    currencyValueRounded: CurrencyValueRounded
): String {
    val numberStr = currencyValueRounded.currency.symbol + App.numberFormatter.format(currencyValueRounded.value.value, 0, Int.MAX_VALUE)
    return when (currencyValueRounded.value) {
        is BigDecimalRounded.Large -> {
            val suffixStr = when (currencyValueRounded.value.suffix) {
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
}
