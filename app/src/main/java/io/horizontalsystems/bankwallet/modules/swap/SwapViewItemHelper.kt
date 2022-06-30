package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import java.math.BigDecimal
import java.math.RoundingMode

class SwapViewItemHelper(private val numberFormatter: IAppNumberFormatter) {

    fun price(price: BigDecimal?, quoteToken: Token?, baseToken: Token?): String? {
        if (price == null || quoteToken == null || baseToken == null)
            return null

        val inversePrice = if (price.compareTo(BigDecimal.ZERO) == 0)
            BigDecimal.ZERO
        else
            BigDecimal.ONE.divide(price, price.scale(), RoundingMode.HALF_UP)

        return "${baseToken.coin.code} = ${coinAmount(inversePrice, quoteToken.coin.code)} "
    }

    fun priceImpactViewItem(
        trade: UniswapTradeService.Trade,
        minLevel: UniswapTradeService.PriceImpactLevel = UniswapTradeService.PriceImpactLevel.Normal
    ): UniswapModule.PriceImpactViewItem? {

        val priceImpact = trade.tradeData.priceImpact ?: return null
        val impactLevel = trade.priceImpactLevel ?: return null
        if (impactLevel < minLevel) {
            return null
        }

        return UniswapModule.PriceImpactViewItem(impactLevel, Translator.getString(R.string.Swap_Percent, priceImpact))
    }

    fun guaranteedAmountViewItem(
        tradeData: TradeData,
        tokenIn: Token?,
        tokenOut: Token?
    ): UniswapModule.GuaranteedAmountViewItem? {
        when (tradeData.type) {
            TradeType.ExactIn -> {
                val amount = tradeData.amountOutMin ?: return null
                val token = tokenOut ?: return null

                return UniswapModule.GuaranteedAmountViewItem(
                    Translator.getString(R.string.Swap_MinimumGot),
                    coinAmount(amount, token.coin.code)
                )
            }
            TradeType.ExactOut -> {
                val amount = tradeData.amountInMax ?: return null
                val token = tokenIn ?: return null

                return UniswapModule.GuaranteedAmountViewItem(
                    Translator.getString(R.string.Swap_MaximumPaid),
                    coinAmount(amount, token.coin.code)
                )
            }
        }
    }

    fun slippage(allowedSlippage: BigDecimal): String? {
        val defaultTradeOptions = TradeOptions()
        return if (allowedSlippage.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0) {
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
            Translator.getString(R.string.Duration_Minutes, ttl / 60)
        }
    }

    fun coinAmount(amount: BigDecimal, coinCode: String): String {
        return numberFormatter.formatCoinFull(amount, coinCode, 8)
    }

}
