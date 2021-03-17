package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService.GasPriceType
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class SendEvmTransactionService(
        private val sendEvmData: SendEvmData,
        private val evmKit: EthereumKit,
        private val transactionsService: EvmTransactionService,
        gasPrice: Long? = null
) : Clearable {
    private val disposable = CompositeDisposable()

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    private val stateSubject = PublishSubject.create<State>()
    var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Flowable<State> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sendStateSubject = PublishSubject.create<SendState>()
    var sendState: SendState = SendState.Idle
        private set(value) {
            field = value
            sendStateSubject.onNext(value)
        }
    val sendStateObservable: Flowable<SendState> = sendStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionData: TransactionData = sendEvmData.transactionData
    val additionalItems: List<SendEvmData.AdditionalItem> = sendEvmData.additionalItems
    val ownAddress: Address = evmKit.receiveAddress
    val decoration: TransactionDecoration? by lazy { evmKit.decorate(sendEvmData.transactionData) }

    init {
        transactionsService.transactionStatusObservable.subscribeIO { syncState() }.let { disposable.add(it) }
        transactionsService.transactionData = sendEvmData.transactionData
        gasPrice?.let { transactionsService.gasPriceType = GasPriceType.Custom(it) }
    }

    fun send() {
        if (state != State.Ready) return
        val transaction = transactionsService.transactionStatus.dataOrNull ?: return

        sendState = SendState.Sending

        evmKit.send(sendEvmData.transactionData, transaction.gasData.gasPrice, transaction.gasData.gasLimit)
                .subscribeIO({ fullTransaction ->
                    sendState = SendState.Sent(fullTransaction.transaction.hash)
                }, { error ->
                    sendState = SendState.Failed(error)
                })
                .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }

    private fun syncState() {
        when (val status = transactionsService.transactionStatus) {
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
    }

    sealed class State {
        object Ready : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
    }

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
