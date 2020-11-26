package io.horizontalsystems.bankwallet.modules.swap_new.repositories

import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import java.math.BigDecimal
import kotlin.Pair

class UniswapRepository(
        private val uniswapKit: UniswapKit
) {
    private val swapDataCache = HashMap<Pair<Coin, Coin>, SwapData>()

    val routerAddress: Address
        get() = uniswapKit.routerAddress

    fun getTradeData(coinFrom: Coin, coinTo: Coin, amount: BigDecimal, tradeType: TradeType, tradeOptions: TradeOptions): Single<TradeData> =
            Single.create { emitter ->
                try {
                    val cacheKey = Pair(coinFrom, coinTo)
                    val swapData: SwapData = swapDataCache[cacheKey] ?: kotlin.run {
                        val tokenIn = uniswapToken(coinFrom)
                        val tokenOut = uniswapToken(coinTo)
                        uniswapKit.swapData(tokenIn, tokenOut).blockingGet().also {
                            swapDataCache[cacheKey] = it
                        }
                    }

                    val tradeData = when (tradeType) {
                        TradeType.ExactIn -> {
                            uniswapKit.bestTradeExactIn(swapData, amount, tradeOptions)
                        }
                        TradeType.ExactOut -> {
                            uniswapKit.bestTradeExactOut(swapData, amount, tradeOptions)
                        }
                    }
                    emitter.onSuccess(tradeData)
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapKit.transactionData(tradeData)
    }

    fun swap(tradeData: TradeData, gasPrice: Long, gasLimit: Long): Single<String> =
            Single.create { emitter ->
                try {
                    val transactionWithInternal = uniswapKit.swap(tradeData, gasPrice, gasLimit).blockingGet()
                    val txHash = transactionWithInternal.transaction.hash.toRawHexString()

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
