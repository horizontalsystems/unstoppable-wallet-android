package io.horizontalsystems.bankwallet.modules.swap.uniswapv3

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ExactType
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapData.UniswapData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.bankwallet.modules.swap.uniswap.IUniswapTradeService
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException
import io.horizontalsystems.uniswapkit.models.Token as UniswapToken

class UniswapV3TradeService(
    private val uniswapV3Kit: UniswapV3Kit
) : IUniswapTradeService {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var tradeDataJob: Job? = null

    private var uniswapTokenFrom: UniswapToken? = null
    private var uniswapTokenTo: UniswapToken? = null

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    override val recipient: Address?
        get() = tradeOptions.recipient
    override val slippage: BigDecimal
        get() = tradeOptions.allowedSlippage
    override val ttl: Long
        get() = tradeOptions.ttl

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    override var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
        }

    override fun stop() = Unit

    override fun fetchSwapData(
        tokenFrom: Token?,
        tokenTo: Token?,
        amountFrom: BigDecimal?,
        amountTo: BigDecimal?,
        exactType: ExactType
    ) {
        if (tokenFrom == null || tokenTo == null) {
            state = SwapResultState.NotReady()
            return
        }

        state = SwapResultState.Loading

        uniswapTokenFrom = uniswapToken(tokenFrom)
        uniswapTokenTo = uniswapToken(tokenTo)
        syncTradeData(exactType, amountFrom, amountTo, tokenFrom, tokenTo)
//        state = SwapResultState.NotReady(listOf(error))
    }

    override fun updateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeOptions = SwapTradeOptions(
            slippage ?: TradeOptions.defaultAllowedSlippage,
            ttl ?: TradeOptions.defaultTtl,
            recipient
        )
    }

    @Throws
    override fun transactionData(tradeData: UniversalSwapTradeData): TransactionData {
        return uniswapV3Kit.transactionData(tradeData.getTradeDataV3())
    }

    private fun syncTradeData(exactType: ExactType, amountFrom: BigDecimal?, amountTo: BigDecimal?, tokenFrom: Token, tokenTo: Token) {
        tradeDataJob?.cancel()
        val uniswapTokenFrom = uniswapTokenFrom ?: return
        val uniswapTokenTo = uniswapTokenTo ?: return

        val amount = if (exactType == ExactType.ExactFrom) amountFrom else amountTo

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = SwapResultState.NotReady()
            return
        }

        tradeDataJob = coroutineScope.launch {
            try {
                val tradeType = when (exactType) {
                    ExactType.ExactFrom -> TradeType.ExactIn
                    ExactType.ExactTo -> TradeType.ExactOut
                }
                val tradeData = tradeData(uniswapTokenFrom, uniswapTokenTo, amount, tradeType, tradeOptions.tradeOptions)
                state = SwapResultState.Ready(UniswapData(tradeData))
            } catch (e: CancellationException) {
                // do nothing
            } catch (e: Throwable) {
                val error = when {
                    e is TradeError.TradeNotFound && isEthWrapping(tokenFrom, tokenTo) -> TradeServiceError.WrapUnwrapNotAllowed
                    else -> e
                }
                state = SwapResultState.NotReady(listOf(error))
            }
        }

    }

    private suspend fun tradeData(
        tokenIn: UniswapToken,
        tokenOut: UniswapToken,
        amount: BigDecimal,
        tradeType: TradeType,
        tradeOptions: TradeOptions
    ): UniversalSwapTradeData {
        val tradeDataV3 = when (tradeType) {
            TradeType.ExactIn -> {
                uniswapV3Kit.bestTradeExactIn(tokenIn, tokenOut, amount, tradeOptions)
            }
            TradeType.ExactOut -> {
                uniswapV3Kit.bestTradeExactOut(tokenIn, tokenOut, amount, tradeOptions)
            }
        }
        return UniversalSwapTradeData.buildFromTradeDataV3(tradeDataV3)
    }

    @Throws
    private fun uniswapToken(token: Token?) = when (val tokenType = token?.type) {
        TokenType.Native -> when (token.blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> uniswapV3Kit.etherToken()
            else -> throw Exception("Invalid coin for swap: $token")
        }
        is TokenType.Eip20 -> uniswapV3Kit.token(
            io.horizontalsystems.ethereumkit.models.Address(
                tokenType.address
            ), token.decimals)
        else -> throw Exception("Invalid coin for swap: $token")
    }

    private val TokenType.isWeth: Boolean
        get() = this is TokenType.Eip20 && address.equals(uniswapV3Kit.etherToken().address.hex, true)
    private val Token.isWeth: Boolean
        get() = type.isWeth
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth || tokenTo.isNative && tokenFrom.isWeth
            }
        }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

}
