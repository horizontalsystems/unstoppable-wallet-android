package io.horizontalsystems.bankwallet.modules.swap.view

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class SwapItemFormatter(
        private val stringProvider: StringProvider,
        private val numberFormatter: IAppNumberFormatter
) {

    fun minMaxTitle(amountType: AmountType): String {
        return if (amountType == AmountType.ExactSending)
            stringProvider.string(R.string.Swap_MinimumReceived)
        else
            stringProvider.string(R.string.Swap_MaximumSold)
    }

    fun minMaxValue(amount: BigDecimal, coinSending: Coin, coinReceiving: Coin, amountType: AmountType): String {
        return when (amountType) {
            AmountType.ExactSending -> {
                coinAmount(amount, coinReceiving)
            }
            AmountType.ExactReceiving -> {
                coinAmount(amount, coinSending)
            }
        }
    }

    fun executionPrice(price: BigDecimal, coinSending: Coin, coinReceiving: Coin): String {
        val inversePrice = if (price.unscaledValue() == BigInteger.ZERO)
            BigDecimal.ZERO
        else
            BigDecimal.ONE.divide(price, price.scale(), RoundingMode.HALF_UP)

        return "${coinReceiving.code} = ${coinAmount(inversePrice, coinSending)} "
    }

    fun price(price: BigDecimal?, coinFrom: Coin?, coinTo: Coin?): String? {
        if (price == null || coinFrom == null || coinTo == null)
            return null
        val inversePrice = if (price.unscaledValue() == BigInteger.ZERO)
            BigDecimal.ZERO
        else
            BigDecimal.ONE.divide(price, price.scale(), RoundingMode.HALF_UP)

        return "${coinTo.code} = ${coinAmount(inversePrice, coinFrom)} "
    }

    fun priceImpactViewItem(
            trade: SwapTradeService.Trade,
            minLevel: SwapTradeService.PriceImpactLevel = SwapTradeService.PriceImpactLevel.Normal
    ): SwapModule.PriceImpactViewItem? {

        val priceImpact = trade.tradeData.priceImpact ?: return null
        val impactLevel = trade.priceImpactLevel ?: return null
        if (impactLevel < minLevel) {
            return null
        }

        return SwapModule.PriceImpactViewItem(impactLevel, stringProvider.string(R.string.Swap_Percent, priceImpact))
    }

    fun priceImpact(priceImpact: BigDecimal?): String? {
        return priceImpact?.toPlainString()?.let { stringProvider.string(R.string.Swap_Percent, it) }
    }

    fun guaranteedAmountViewItem(tradeData: TradeData, coinIn: Coin?, coinOut: Coin?): SwapModule.GuaranteedAmountViewItem? {
        when (tradeData.type) {
            TradeType.ExactIn -> {
                val amount = tradeData.amountOutMin ?: return null
                val coin = coinOut ?: return null

                return SwapModule.GuaranteedAmountViewItem(stringProvider.string(R.string.Swap_MinimumGot), coinAmount(amount, coin))
            }
            TradeType.ExactOut -> {
                val amount = tradeData.amountInMax ?: return null
                val coin = coinIn ?: return null

                return SwapModule.GuaranteedAmountViewItem(stringProvider.string(R.string.Swap_MaximumPaid), coinAmount(amount, coin))
            }
        }
    }

    fun priceImpact(priceImpact: PriceImpact): String {
        return stringProvider.string(R.string.Swap_Percent, priceImpact.value.toPlainString())
    }

    fun fee(fee: Pair<CoinValue, CurrencyValue?>): String {
        val coinAmount = fee.first.let { coinAmount(it.value, it.coin) }
        val fiatAmount = fee.second?.let {
            numberFormatter.formatFiat(it.value, it.currency.symbol, 2, 2)
        }
        return "$coinAmount${if (fiatAmount != null) " | $fiatAmount" else ""}"
    }

    fun coinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return coinAmount(amount, coin, maxFraction)
    }

    fun coinAmount(amount: BigDecimal, coin: Coin, maxFraction: Int): String {
        return numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }

    fun slippage(allowedSlippage: BigDecimal): String? {
        val defaultTradeOptions = TradeOptions()
        return if (allowedSlippage.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0){
            null
        } else {
            "$allowedSlippage%"
        }
    }

    fun deadline(ttl: Long): String? {
        val defaultTradeOptions = TradeOptions()
        return if (ttl == defaultTradeOptions.ttl) {
            null
        } else {
            stringProvider.string(R.string.Duration_Minutes, ttl)
        }
    }

}
