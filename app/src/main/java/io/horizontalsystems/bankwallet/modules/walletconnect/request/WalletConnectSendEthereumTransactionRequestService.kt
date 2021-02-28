package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class WalletConnectSendEthereumTransactionRequestService(
        private val request: WalletConnectSendEthereumTransactionRequest,
        private val baseService: WalletConnectService,
        private val transactionService: EvmTransactionService,
        private val evmKit: EthereumKit
) {
    private val disposable = CompositeDisposable()
    private val transaction = request.transaction

    val transactionData = TransactionData(transaction.to, transaction.value, transaction.data)

    private val stateSubject = PublishSubject.create<State>()
    var state: State = State.NotReady(null)
        set(value) {
            field = value

            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    init {
        transaction.gasPrice?.let {
            transactionService.gasPriceType = EvmTransactionService.GasPriceType.Custom(it)
        }

        transactionService.transactionStatusObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncState()
                }
                .let {
                    disposable.add(it)
                }

        transactionService.transactionData = transactionData
    }

    fun approve() {
        if (state != State.Ready) return
        val transaction = transactionService.transactionStatus.dataOrNull ?: return

        state = State.Sending

        evmKit.send(
                transaction.data.to,
                transaction.data.value,
                transaction.data.input,
                transaction.gasData.gasPrice,
                transaction.gasData.gasLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ fullTransaction ->
                    handleSent(fullTransaction.transaction.hash)
                }, {
                    state = State.NotReady(it)
                }).let {
                    disposable.add(it)
                }
    }

    fun reject() {
        baseService.rejectRequest(request.id)
    }

    private fun handleSent(transactionHash: ByteArray) {
        baseService.approveRequest(request.id, transactionHash)
        state = State.Sent
    }

    private fun syncState() {
        state = when (val transactionStatus = transactionService.transactionStatus) {
            DataState.Loading -> {
                State.NotReady(null)
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                val balance = evmKit.accountState?.balance ?: BigInteger.ZERO

                if (transaction.totalAmount > balance) {
                    State.NotReady(TransactionError.InsufficientBalance(transaction.totalAmount))
                } else {
                    State.Ready
                }
            }
            is DataState.Error -> {
                State.NotReady(transactionStatus.error)
            }
        }
    }

    sealed class State {
        object Ready : State()
        class NotReady(val error: Throwable?) : State()
        object Sending : State()
        object Sent : State()
    }

    sealed class TransactionError : Error() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionError()
    }

}
