package io.horizontalsystems.bankwallet.modules.swap.providers

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import java.math.BigDecimal
import kotlin.jvm.Throws

class UniswapProvider(private val uniswapKit: UniswapKit) {

    val routerAddress: Address
        get() = uniswapKit.routerAddress

    fun swapDataSingle(coinIn: Coin, coinOut: Coin): Single<SwapData> {
        return try {
            val tokenIn = uniswapToken(coinIn)
            val tokenOut = uniswapToken(coinOut)

            uniswapKit.swapData(tokenIn, tokenOut)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    fun tradeData(swapData: SwapData, amount: BigDecimal, tradeType: TradeType, tradeOptions: TradeOptions): TradeData {
        return when (tradeType) {
            TradeType.ExactIn -> {
                uniswapKit.bestTradeExactIn(swapData, amount, tradeOptions)
            }
            TradeType.ExactOut -> {
                uniswapKit.bestTradeExactOut(swapData, amount, tradeOptions)
            }
        }
    }

    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapKit.transactionData(tradeData)
    }

    @Throws
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
