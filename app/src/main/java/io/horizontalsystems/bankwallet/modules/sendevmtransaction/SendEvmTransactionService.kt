package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService.GasPriceType
import io.horizontalsystems.bankwallet.core.managers.ActivateCoinManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

interface ISendEvmTransactionService {
    val state: SendEvmTransactionService.State
    val stateObservable: Flowable<SendEvmTransactionService.State>

    val txDataState: DataState<SendEvmTransactionService.TxDataState>
    val txDataStateObservable: Flowable<DataState<SendEvmTransactionService.TxDataState>>

    val sendState: SendEvmTransactionService.SendState
    val sendStateObservable: Flowable<SendEvmTransactionService.SendState>

    val ownAddress: Address

    fun send(logger: AppLogger)
}

class SendEvmTransactionService(
        private val sendEvmData: SendEvmData,
        private val evmKit: EthereumKit,
        private val transactionService: EvmTransactionService,
        private val activateCoinManager: ActivateCoinManager,
        gasPrice: Long? = null
) : Clearable, ISendEvmTransactionService {
    private val disposable = CompositeDisposable()

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

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

    private val txDataStateSubject = PublishSubject.create<DataState<TxDataState>>()
    override var txDataState: DataState<TxDataState> = DataState.Success(TxDataState(sendEvmData.transactionData, sendEvmData.additionalInfo, evmKit.decorate(sendEvmData.transactionData)))
        private set(value) {
            field = value
            txDataStateSubject.onNext(value)
        }
    override val txDataStateObservable: Flowable<DataState<TxDataState>> = txDataStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val ownAddress: Address = evmKit.receiveAddress

    init {
        transactionService.transactionStatusObservable.subscribeIO { syncState() }.let { disposable.add(it) }
        transactionService.setTransactionData(sendEvmData.transactionData)
        gasPrice?.let { transactionService.gasPriceType = GasPriceType.Custom(it) }
    }

    override fun send(logger: AppLogger) {
        if (state != State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }
        val transaction = transactionService.transactionStatus.dataOrNull ?: return

        sendState = SendState.Sending
        logger.info("sending tx")

        evmKit.send(transaction.data, transaction.gasData.gasPrice, transaction.gasData.gasLimit)
                .subscribeIO({ fullTransaction ->
                    handlePostSendActions()
                    sendState = SendState.Sent(fullTransaction.transaction.hash)
                    logger.info("success txHash: ${fullTransaction.transaction.hash.toHexString()}")
                }, { error ->
                    sendState = SendState.Failed(error)
                    logger.warning("failed", error)
                })
                .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }

    private fun syncState() {
        when (val status = transactionService.transactionStatus) {
            is DataState.Error -> {
                state = State.NotReady(listOf(status.error))
            }
            DataState.Loading -> {
                state = State.NotReady()
            }
            is DataState.Success -> {
                val transaction = status.data
                state = if (transaction.totalAmount > evmBalance) {
                    State.NotReady(listOf(TransactionError.InsufficientBalance(transaction.totalAmount)))
                } else {
                    State.Ready
                }
            }
        }
        syncTxDataState(transactionService.transactionStatus)
    }

    private fun syncTxDataState(transactionDataState: DataState<EvmTransactionService.Transaction>) {
        val transactionData = transactionDataState.dataOrNull?.data ?: sendEvmData.transactionData
        txDataState = DataState.Success(TxDataState(transactionData, sendEvmData.additionalInfo, evmKit.decorate(transactionData)))
    }

    private fun handlePostSendActions() {
        (txDataState.dataOrNull?.decoration as? SwapMethodDecoration)?.let { swapDecoration ->
            activateSwapCoinOut(swapDecoration.tokenOut)
        }
    }

    private fun activateSwapCoinOut(tokenOut: SwapMethodDecoration.Token) {
        val coinType = when (tokenOut) {
            SwapMethodDecoration.Token.EvmCoin -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.EthMainNet,
                    EthereumKit.NetworkType.EthRopsten,
                    EthereumKit.NetworkType.EthKovan,
                    EthereumKit.NetworkType.EthGoerli,
                    EthereumKit.NetworkType.EthRinkeby -> CoinType.Ethereum
                    EthereumKit.NetworkType.BscMainNet -> CoinType.BinanceSmartChain
                }
            }
            is SwapMethodDecoration.Token.Eip20Coin -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.EthMainNet,
                    EthereumKit.NetworkType.EthRopsten,
                    EthereumKit.NetworkType.EthKovan,
                    EthereumKit.NetworkType.EthGoerli,
                    EthereumKit.NetworkType.EthRinkeby -> CoinType.Erc20(tokenOut.address.hex)
                    EthereumKit.NetworkType.BscMainNet -> CoinType.Bep20(tokenOut.address.hex)
                }
            }
        }

        activateCoinManager.activate(coinType)
    }

    sealed class State {
        object Ready : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
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
