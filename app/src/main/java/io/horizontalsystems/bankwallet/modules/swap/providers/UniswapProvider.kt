package io.horizontalsystems.bankwallet.modules.swap.providers

import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import java.math.BigDecimal

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
            is CoinType.Erc20 -> {
                uniswapKit.token(Address(coinType.address), coin.decimal)
            }
            is CoinType.Bep20 -> {
                uniswapKit.token(Address(coinType.address), coin.decimal)
            }
            CoinType.Ethereum, CoinType.BinanceSmartChain -> {
                uniswapKit.etherToken()
            }
            else -> throw Exception("Invalid coin for swap: $coin")
        }
    }
}
