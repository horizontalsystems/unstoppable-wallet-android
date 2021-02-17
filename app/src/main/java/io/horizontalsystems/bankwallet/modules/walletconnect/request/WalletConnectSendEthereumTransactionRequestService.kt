package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class WalletConnectSendEthereumTransactionRequestService(
        private val transaction: WalletConnectTransaction,
        private val transactionService: EvmTransactionService,
        private val ethereumKit: EthereumKit
) {
    val transactionData = TransactionData(transaction.to, transaction.value, transaction.data)

    var state: State = State.NotReady(null)
        set(value) {
            field = value

            stateSubject.onNext(value)
        }
    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Observable<State> = stateSubject

    private val disposable = CompositeDisposable()

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

    fun send() {
        if (state != State.Ready) return
        val transaction = transactionService.transactionStatus.dataOrNull ?: return

        state = State.Sending

        ethereumKit.send(
                transaction.data.to,
                transaction.data.value,
                transaction.data.input,
                transaction.gasData.gasPrice,
                transaction.gasData.gasLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ fullTransaction ->
                    state = State.Sent(fullTransaction.transaction.hash)
                }, {
                    state = State.NotReady(it)
                }).let {
                    disposable.add(it)
                }
    }

    private fun syncState() {
        state = when (val transactionStatus = transactionService.transactionStatus) {
            DataState.Loading -> {
                State.NotReady(null)
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                val balance = ethereumKit.accountState?.balance ?: BigInteger.ZERO

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
        class Sent(val transactionHash: ByteArray) : State()
    }

    sealed class TransactionError : Error() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionError()
    }

}
