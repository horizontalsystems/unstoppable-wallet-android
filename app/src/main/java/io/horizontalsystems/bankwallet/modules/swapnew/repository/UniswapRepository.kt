package io.horizontalsystems.bankwallet.modules.swapnew.repository

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swapnew.DataState
import io.horizontalsystems.bankwallet.modules.swapnew.model.AmountType
import io.horizontalsystems.bankwallet.modules.swapnew.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swapnew.model.Trade
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.math.BigDecimal

class UniswapRepository(
        private val uniswapKit: UniswapKit
) {
    private val swapDataCache = HashMap<Pair<Coin, Coin>, SwapData>()
    private val priceImpactDesirableThreshold = BigDecimal("1")
    private val priceImpactAllowedThreshold = BigDecimal("5")

    fun trade(
            coinSending: Coin,
            coinReceiving: Coin,
            amount: BigDecimal,
            amountType: AmountType
    ): Flowable<DataState<Trade>> = Flowable.create({ emitter ->
        val cacheKey = Pair(coinSending, coinReceiving)
        try {
            val swapData: SwapData = swapDataCache[cacheKey] ?: kotlin.run {

                emitter.onNext(DataState.Loading)

                val tokenIn = uniswapToken(coinSending)
                val tokenOut = uniswapToken(coinReceiving)
                uniswapKit.swapData(tokenIn, tokenOut).blockingGet().also {
                    swapDataCache[cacheKey] = it
                }
            }

            val tradeData = when (amountType) {
                AmountType.ExactSending -> {
                    uniswapKit.bestTradeExactIn(swapData, amount)
                }
                AmountType.ExactReceiving -> {
                    uniswapKit.bestTradeExactOut(swapData, amount)
                }
            }
            emitter.onNext(DataState.Success(swapModel(coinSending, coinReceiving, tradeData)))

        } catch (error: Throwable) {
            emitter.onNext(DataState.Error(error))
        }
        emitter.onComplete()
    }, BackpressureStrategy.BUFFER)


    private fun swapModel(fromCoin: Coin, toCoin: Coin, tradeData: TradeData): Trade {
        tradeData.apply {
            return Trade(
                    fromCoin,
                    toCoin,
                    if (tradeData.type == TradeType.ExactIn) AmountType.ExactSending else AmountType.ExactReceiving,
                    amountIn,
                    amountOut,
                    executionPrice,
                    priceImpact(priceImpact),
                    if (tradeData.type == TradeType.ExactIn) amountOutMin else amountInMax
            )
        }
    }

    private fun priceImpact(value: BigDecimal?): PriceImpact? {
        return value?.let {
            when {
                value < priceImpactDesirableThreshold -> PriceImpact(value, PriceImpact.Level.Normal)
                value < priceImpactAllowedThreshold -> PriceImpact(value, PriceImpact.Level.Warning)
                else -> PriceImpact(value, PriceImpact.Level.Forbidden)
            }
        }
    }

    private fun uniswapToken(coin: Coin): Token {
        return when (val coinType = coin.type) {
            is CoinType.Ethereum -> uniswapKit.etherToken()
            is CoinType.Erc20 -> {
                uniswapKit.token(Address(coinType.address), coin.decimal)
            }
            else -> throw Exception("Invalid coin for swap: $coin")
        }
    }

}
