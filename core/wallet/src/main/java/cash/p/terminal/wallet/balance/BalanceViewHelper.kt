package cash.p.terminal.wallet.balance

import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.wallet.models.CoinPrice
import org.koin.java.KoinJavaComponent.inject
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
        val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
        val formatted = if (fullFormat) {
            numberFormatter.formatCoinFull(balance, null, coinDecimals)
        } else {
            numberFormatter.formatCoinShort(balance, null, coinDecimals)
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
            val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
            if (fullFormat) {
                numberFormatter.formatFiatFull(balanceFiat, currency.symbol)
            } else {
                numberFormatter.formatFiatShort(balanceFiat, currency.symbol, 8)
            }
        } ?: ""

        return DeemedValue(formatted, dimmedOrExpired, visible)
    }

    fun rateValue(coinPrice: CoinPrice?, currency: Currency, visible: Boolean): DeemedValue<String> {
        val value = coinPrice?.let {
            val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
            numberFormatter.formatFiatFull(coinPrice.value, currency.symbol)
        } ?: ""

        return DeemedValue(value, dimmed = coinPrice?.expired ?: false, visible = visible)
    }

}