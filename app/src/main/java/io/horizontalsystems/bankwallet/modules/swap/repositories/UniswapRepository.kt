package io.horizontalsystems.bankwallet.modules.swap.repositories

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

    fun getTradeData(coinFrom: Coin, coinTo: Coin, amount: BigDecimal, tradeType: TradeType, tradeOptions: TradeOptions, forcedSync: Boolean): Single<TradeData> =
            Single.create { emitter ->
                try {
                    val cacheKey = Pair(coinFrom, coinTo)

                    if (forcedSync || swapDataCache[cacheKey] == null){
                        val swapData = swapData(coinFrom, coinTo).blockingGet()
                        val tradeData = tradeData(tradeType, swapData, amount, tradeOptions)
                        emitter.onSuccess(tradeData)

                        swapDataCache[cacheKey] = swapData
                    }

                    swapDataCache[cacheKey]?.let {
                        val tradeData = tradeData(tradeType, it, amount, tradeOptions)
                        emitter.onSuccess(tradeData)
                    }
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    private fun tradeData(tradeType: TradeType, swapData: SwapData, amount: BigDecimal, tradeOptions: TradeOptions): TradeData {
        return when (tradeType) {
            TradeType.ExactIn -> {
                uniswapKit.bestTradeExactIn(swapData, amount, tradeOptions)
            }
            TradeType.ExactOut -> {
                uniswapKit.bestTradeExactOut(swapData, amount, tradeOptions)
            }
        }
    }

    private fun swapData(coinFrom: Coin, coinTo: Coin): Single<SwapData> {
        val tokenIn = uniswapToken(coinFrom)
        val tokenOut = uniswapToken(coinTo)
        return uniswapKit.swapData(tokenIn, tokenOut)
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
