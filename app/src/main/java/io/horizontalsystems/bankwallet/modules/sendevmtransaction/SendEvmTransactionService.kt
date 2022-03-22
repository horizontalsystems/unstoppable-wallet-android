package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.managers.ActivateCoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

interface ISendEvmTransactionService {
    val state: SendEvmTransactionService.State
    val stateObservable: Flowable<SendEvmTransactionService.State>

    val txDataState: SendEvmTransactionService.TxDataState

    val sendState: SendEvmTransactionService.SendState
    val sendStateObservable: Flowable<SendEvmTransactionService.SendState>

    val ownAddress: Address

    fun send(logger: AppLogger)
}

class SendEvmTransactionService(
    private val sendEvmData: SendEvmData,
    private val evmKitWrapper: EvmKitWrapper,
    private val feeService: IEvmFeeService,
    private val activateCoinManager: ActivateCoinManager
) : Clearable, ISendEvmTransactionService {
    private val disposable = CompositeDisposable()

    private val evmKit = evmKitWrapper.evmKit
    private val stateSubject = PublishSubject.create<State>()

    override var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateObservable: Flowable<State> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sendStateSubject = PublishSubject.create<SendState>()
    override var sendState: SendState = SendState.Idle
        private set(value) {
            field = value
            sendStateSubject.onNext(value)
        }
    override val sendStateObservable: Flowable<SendState> = sendStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override var txDataState: TxDataState = TxDataState(
        sendEvmData.transactionData,
        sendEvmData.additionalInfo,
        evmKit.decorate(sendEvmData.transactionData)
    )
        private set

    override val ownAddress: Address = evmKit.receiveAddress

    init {
        feeService.transactionStatusObservable
            .subscribeIO { sync(it) }
            .let { disposable.add(it) }
    }

    private fun sync(transactionStatus: DataState<Transaction>) {
        when (transactionStatus) {
            is DataState.Error -> {
                state = State.NotReady(errors = listOf(transactionStatus.error))
                syncTxDataState()
            }
            DataState.Loading -> {
                state = State.NotReady()
            }
            is DataState.Success -> {
                syncTxDataState(transactionStatus.data)

                val warnings = transactionStatus.data.warnings + sendEvmData.warnings
                state = if (transactionStatus.data.errors.isNotEmpty()) {
                    State.NotReady(warnings, transactionStatus.data.errors)
                } else {
                    State.Ready(warnings)
                }
            }
        }
    }

    override fun send(logger: AppLogger) {
        if (state !is State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }
        val transaction = feeService.transactionStatus.dataOrNull ?: return

        sendState = SendState.Sending
        logger.info("sending tx")

        evmKitWrapper.sendSingle(
            transaction.transactionData,
            transaction.gasData.gasPrice,
            transaction.gasData.gasLimit,
            transaction.transactionData.nonce
        )
            .subscribeIO({ fullTransaction ->
                handlePostSendActions()
                sendState = SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                sendState = SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }

    private fun syncTxDataState(transaction: Transaction? = null) {
        val transactionData = transaction?.transactionData ?: sendEvmData.transactionData
        txDataState = TxDataState(transactionData, sendEvmData.additionalInfo, evmKit.decorate(transactionData))
    }

    private fun handlePostSendActions() {
        (txDataState.decoration as? SwapMethodDecoration)?.let { swapMethodDecoration ->
            activateUniswapCoins(swapMethodDecoration)
        }
        (txDataState.decoration as? OneInchMethodDecoration)?.let { oneInchMethodDecoration ->
            activateOneInchSwapCoins(oneInchMethodDecoration, txDataState.additionalInfo)
        }
    }

    private fun activateUniswapCoins(swapMethodDecoration: SwapMethodDecoration) {
        val fromCoinType = mapUniswapTokenToCoinType(swapMethodDecoration.tokenIn)
        val toCoinType = mapUniswapTokenToCoinType(swapMethodDecoration.tokenOut)

        activateCoinManager.activate(fromCoinType)
        activateCoinManager.activate(toCoinType)
    }

    private fun activateOneInchSwapCoins(
        decoration: OneInchMethodDecoration,
        additionalInfo: SendEvmData.AdditionalInfo?
    ) {
        val fromCoinType: CoinType? = when (decoration) {
            is OneInchSwapMethodDecoration -> {
                mapOneInchTokenToCoinType(decoration.fromToken)
            }
            is OneInchUnoswapMethodDecoration -> {
                mapOneInchTokenToCoinType(decoration.fromToken)
            }
            else -> null
        }

        val toCoinType: CoinType? = when (decoration) {
            is OneInchSwapMethodDecoration -> {
                mapOneInchTokenToCoinType(decoration.toToken)
            }
            is OneInchUnoswapMethodDecoration -> {
                val coinType = decoration.toToken?.let {
                    mapOneInchTokenToCoinType(it)
                }
                coinType ?: additionalInfo?.oneInchSwapInfo?.coinTo?.coinType
            }
            else -> null
        }

        fromCoinType?.let { activateCoinManager.activate(it) }
        toCoinType?.let { activateCoinManager.activate(it) }
    }

    private fun mapOneInchTokenToCoinType(token: OneInchMethodDecoration.Token) = when (token) {
        OneInchMethodDecoration.Token.EvmCoin -> {
            getEvmCoinType()
        }
        is OneInchMethodDecoration.Token.Eip20 -> {
            getEip20CoinType(token.address.hex)
        }
    }

    private fun mapUniswapTokenToCoinType(token: SwapMethodDecoration.Token) = when (token) {
        SwapMethodDecoration.Token.EvmCoin -> {
            getEvmCoinType()
        }
        is SwapMethodDecoration.Token.Eip20Coin -> {
            getEip20CoinType(token.address.hex)
        }
    }

    private fun getEip20CoinType(contractAddress: String) =
        when (evmKit.chain) {
            Chain.BinanceSmartChain -> CoinType.Bep20(contractAddress)
            else -> CoinType.Erc20(contractAddress)
        }

    private fun getEvmCoinType() = when (evmKit.chain) {
        Chain.BinanceSmartChain -> CoinType.BinanceSmartChain
        else -> CoinType.Ethereum
    }

    sealed class State {
        class Ready(val warnings: List<Warning> = listOf()) : State()
        class NotReady(val warnings: List<Warning> = listOf(), val errors: List<Throwable> = listOf()) : State()
    }

    data class TxDataState(
        val transactionData: TransactionData?,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val decoration: ContractMethodDecoration?
    )

    sealed class SendState {
        object Idle : SendState()
        object Sending : SendState()
        class Sent(val transactionHash: ByteArray) : SendState()
        class Failed(val error: Throwable) : SendState()
    }

    sealed class TransactionError : Throwable() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionError()
    }

}
