package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
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
    fun methodName(input: ByteArray): String?
}

class SendEvmTransactionService(
    private val sendEvmData: SendEvmData,
    private val evmKitWrapper: EvmKitWrapper,
    private val feeService: IEvmFeeService,
    private val evmLabelManager: EvmLabelManager
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
                sendState = SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                sendState = SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }
    }

    override fun methodName(input: ByteArray): String? =
        evmLabelManager.methodLabel(input)

    override fun clear() {
        disposable.clear()
    }

    private fun syncTxDataState(transaction: Transaction? = null) {
        val transactionData = transaction?.transactionData ?: sendEvmData.transactionData
        txDataState = TxDataState(transactionData, sendEvmData.additionalInfo, evmKit.decorate(transactionData))
    }

    sealed class State {
        class Ready(val warnings: List<Warning> = listOf()) : State()
        class NotReady(val warnings: List<Warning> = listOf(), val errors: List<Throwable> = listOf()) : State()
    }

    data class TxDataState(
        val transactionData: TransactionData?,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val decoration: TransactionDecoration?
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
