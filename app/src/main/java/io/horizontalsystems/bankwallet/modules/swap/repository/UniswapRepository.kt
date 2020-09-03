package io.horizontalsystems.bankwallet.modules.swap.repository

import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.reactivex.Single
import java.math.BigDecimal

class UniswapRepository(
        private val uniswapKit: UniswapKit
) {
    private val swapDataCache = HashMap<Pair<Coin, Coin>, SwapData>()

    val routerAddress: Address
        get() = uniswapKit.routerAddress

    fun getTradeData(coinSending: Coin, coinReceiving: Coin, amount: BigDecimal, amountType: AmountType): Single<TradeData> =
            Single.create { emitter ->
                try {
                    val cacheKey = Pair(coinSending, coinReceiving)
                    val swapData: SwapData = swapDataCache[cacheKey] ?: kotlin.run {
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
                    emitter.onSuccess(tradeData)
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    fun swap(tradeData: TradeData, gasPrice: Long, gasLimit: Long): Single<String> =
            Single.create { emitter ->
                try {
                    val transactionWithInternal = uniswapKit.swap(tradeData, gasPrice, gasLimit).blockingGet()
                    val txHash = transactionWithInternal.transaction.hash.toHexString()

                    emitter.onSuccess(txHash)
                } catch (error: Throwable) {
                    emitter.onError(error)
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
