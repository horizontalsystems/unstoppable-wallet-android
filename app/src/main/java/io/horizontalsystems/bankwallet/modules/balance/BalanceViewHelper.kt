package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinPrice
import java.math.BigDecimal

object BalanceViewHelper {

    fun getPrimaryAndSecondaryValues(
        balance: BigDecimal,
        visible: Boolean,
        fullFormat: Boolean,
        coinDecimals: Int,
        dimmed: Boolean,
        coinPrice: CoinPrice?,
        currency: Currency,
        balanceViewType: BalanceViewType
    ): Pair<DeemedValue<String>, DeemedValue<String>> {
        val coinValueStr = coinValue(
            balance = balance,
            visible = visible,
            fullFormat = fullFormat,
            coinDecimals = coinDecimals,
            dimmed = dimmed
        )
        val currencyValueStr = currencyValue(
            balance = balance,
            coinPrice = coinPrice,
            visible = visible,
            fullFormat = fullFormat,
            currency = currency,
            dimmed = dimmed
        )

        val primaryValue: DeemedValue<String>
        val secondaryValue: DeemedValue<String>
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
        visible: Boolean,
        fullFormat: Boolean,
        coinDecimals: Int,
        dimmed: Boolean
    ): DeemedValue<String> {
        val formatted = if (fullFormat) {
            App.numberFormatter.formatCoinFull(balance, null, coinDecimals)
        } else {
            App.numberFormatter.formatCoinShort(balance, null, coinDecimals)
        }

        return DeemedValue(formatted, dimmed, visible)
    }

    fun currencyValue(
        balance: BigDecimal,
        coinPrice: CoinPrice?,
        visible: Boolean,
        fullFormat: Boolean,
        currency: Currency,
        dimmed: Boolean
    ): DeemedValue<String> {
        val dimmedOrExpired = dimmed || coinPrice?.expired ?: false
        val formatted = coinPrice?.value?.let { rate ->
            val balanceFiat = balance.multiply(rate)

            if (fullFormat) {
                App.numberFormatter.formatFiatFull(balanceFiat, currency.symbol)
            } else {
                App.numberFormatter.formatFiatShort(balanceFiat, currency.symbol, 8)
            }
        } ?: ""

        return DeemedValue(formatted, dimmedOrExpired, visible)
    }

    fun rateValue(coinPrice: CoinPrice?, currency: Currency, visible: Boolean): DeemedValue<String> {
        val value = coinPrice?.let {
            App.numberFormatter.formatFiatFull(coinPrice.value, currency.symbol)
        } ?: ""

        return DeemedValue(value, dimmed = coinPrice?.expired ?: false, visible = visible)
    }

}