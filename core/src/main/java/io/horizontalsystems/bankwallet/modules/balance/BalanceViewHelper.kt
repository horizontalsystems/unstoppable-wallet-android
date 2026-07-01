package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinPrice
import java.math.BigDecimal

object BalanceViewHelper {

    fun getPrimaryAndSecondaryValues(
        balance: BigDecimal,
        fullFormat: Boolean,
        coinDecimals: Int,
        dimmed: Boolean,
        coinPrice: CoinPrice?,
        currency: Currency,
        balanceViewType: BalanceViewType
    ): Pair<DeemedValue<String>?, DeemedValue<String>?> {
        val coinValueStr = coinValue(
            balance = balance,
            fullFormat = fullFormat,
            coinDecimals = coinDecimals,
            dimmed = dimmed
        )
        val currencyValueStr = coinPrice?.let {
            currencyValue(
                balance = balance,
                coinPrice = it,
                fullFormat = fullFormat,
                currency = currency,
                dimmed = dimmed
            )
        }

        val primaryValue: DeemedValue<String>?
        val secondaryValue: DeemedValue<String>?
        when (balanceViewType) {
            BalanceViewType.CoinThenFiat -> {
                primaryValue = coinValueStr
                secondaryValue = currencyValueStr
            }

            BalanceViewType.FiatThenCoin -> {
                primaryValue = currencyValueStr
                secondaryValue = coinValueStr
            }
        }
        return Pair(primaryValue, secondaryValue)
    }

    fun coinValue(
        balance: BigDecimal,
        fullFormat: Boolean,
        coinDecimals: Int,
        dimmed: Boolean
    ): DeemedValue<String> {
        val formatted = if (fullFormat) {
            App.numberFormatter.formatCoinFull(balance, null, coinDecimals)
        } else {
            App.numberFormatter.formatCoinShort(balance, null, coinDecimals)
        }

        return DeemedValue(formatted, dimmed)
    }

    fun currencyValue(
        balance: BigDecimal,
        coinPrice: CoinPrice,
        fullFormat: Boolean,
        currency: Currency,
        dimmed: Boolean
    ): DeemedValue<String> {
        val dimmedOrExpired = dimmed || coinPrice.expired
        val rate = coinPrice.value
        val balanceFiat = balance.multiply(rate)

        val formatted = if (fullFormat) {
            App.numberFormatter.formatFiatFull(balanceFiat, currency.symbol)
        } else {
            App.numberFormatter.formatFiatShort(balanceFiat, currency.symbol, 8)
        }

        return DeemedValue(formatted, dimmedOrExpired)
    }

    fun rateValue(coinPrice: CoinPrice, currency: Currency): DeemedValue<String> {
        val value = App.numberFormatter.formatFiatFull(coinPrice.value, currency.symbol)

        return DeemedValue(value, dimmed = coinPrice.expired)
    }

}