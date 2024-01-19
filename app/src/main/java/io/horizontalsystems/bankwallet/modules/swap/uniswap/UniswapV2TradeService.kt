package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.evmfee.EvmCommonGasDataService
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.SwapQuote
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapFeeField
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import io.horizontalsystems.ethereumkit.models.Address as EthereumKitAddress

class UniswapV2TradeService : IUniswapTradeService {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    private var swapDataDisposable: Disposable? = null
    private var swapData: SwapData? = null

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

    override fun stop() {
        clearDisposables()
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote {
        val swapData = swapDataSingle(tokenIn, tokenOut).await()
        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions.tradeOptions)
        val amountOut = tradeData.amountOut!!
        val fields = mutableListOf<SwapDataField>()

        val feeAmountData = App.evmBlockchainManager.getBaseToken(tokenIn.blockchainType)?.let { baseToken ->
            getFeeData(tokenIn.blockchainType, tradeData)?.let { transaction ->
                val coinServiceFactory = EvmCoinServiceFactory(
                    baseToken,
                    App.marketKit,
                    App.currencyManager,
                    App.coinManager
                )

                val coinService = coinServiceFactory.baseCoinService
                coinService.amountData(
                    transaction.gasData.estimatedFee,
                    transaction.gasData.isSurcharged
                )
            }
        }

        feeAmountData?.let {
            fields.add(SwapFeeField(feeAmountData))
        }


        return SwapQuote(amountOut, fields, feeAmountData)
    }

    private suspend fun getFeeData(
        blockchainType: BlockchainType,
        tradeData: TradeData,
    ): Transaction? {
        val evmKitWrapper = App.evmBlockchainManager
            .getEvmKitManager(blockchainType)
            .evmKitWrapper ?: return null

        val evmKit = evmKitWrapper.evmKit
        val gasPriceService = if (evmKit.chain.isEIP1559Supported) {
            val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
            Eip1559GasPriceService(gasPriceProvider, evmKit)
        } else {
            val gasPriceProvider = LegacyGasPriceProvider(evmKit)
            LegacyGasPriceService(gasPriceProvider)
        }

        val gasDataService = EvmCommonGasDataService.instance(
            evmKitWrapper.evmKit,
            evmKitWrapper.blockchainType
        )

        val chain = App.evmBlockchainManager.getChain(blockchainType)

        val transactionData = uniswapKit.transactionData(
            evmKitWrapper.evmKit.receiveAddress,
            chain,
            tradeData
        )

        val evmFeeService = EvmFeeService(
            evmKitWrapper.evmKit,
            gasPriceService,
            gasDataService,
            transactionData
        )
        val transactionDataState = evmFeeService.transactionStatusObservable
            .asFlow()
            .firstOrNull { !it.loading }

        return transactionDataState?.dataOrNull
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
        TODO()
//        return uniswapKit.transactionData(tradeData.getTradeDataV2())
    }

    private fun clearDisposables() {
        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun swapDataSingle(tokenIn: Token, tokenOut: Token): Single<SwapData> {
        return try {
            val blockchainType = tokenIn.blockchainType
            val chain = App.evmBlockchainManager.getChain(blockchainType)
            val httpSyncSource = App.evmSyncSourceManager.getHttpSyncSource(blockchainType)
            val httpRpcSource = httpSyncSource?.rpcSource as? RpcSource.Http
                ?: throw IllegalStateException("No HTTP RPC Source for blockchain $blockchainType")

            val uniswapTokenIn = uniswapToken(tokenIn, chain)
            val uniswapTokenOut = uniswapToken(tokenOut, chain)

            uniswapKit.swapData(httpRpcSource, chain, uniswapTokenIn, uniswapTokenOut)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> uniswapKit.etherToken(chain)
        is TokenType.Eip20 -> {
            uniswapKit.token(EthereumKitAddress(tokenType.address), token.decimals)
        }

        else -> throw Exception("Invalid coin for swap: $token")
    }

    private fun TokenType.isWeth(chain: Chain): Boolean {
        return this is TokenType.Eip20 && address.equals(uniswapKit.etherToken(chain).address.hex, true)
    }

    private fun Token.isWeth(chain: Chain): Boolean = type.isWeth(chain)
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?, chain: Chain) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth(chain) || tokenTo.isNative && tokenFrom.isWeth(chain)
            }
        }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

}
