package io.horizontalsystems.bankwallet.modules.swap.approve

import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigInteger

class SwapApproveService(
        private val transactionService: EvmTransactionService,
        private val erc20Kit: Erc20Kit,
        private val ethereumKit: EthereumKit,
        amount: BigInteger,
        private val spenderAddress: Address,
        private val allowance: BigInteger,
) : ISwapApproveService {

    override val stateObservable = BehaviorSubject.createDefault<State>(State.ApproveNotAllowed(listOf()))
    override var amount: BigInteger? = amount
        set(value) {
            field = value

            when (value) {
                null -> syncState()
                else -> syncTransactionData(value)
            }
        }

    private var state: State = State.ApproveNotAllowed(listOf())
        set(value) {
            field = value

            stateObservable.onNext(value)
        }

    private val ethereumBalance: BigInteger
        get() = ethereumKit.accountState?.balance ?: BigInteger.ZERO

    private val disposables = CompositeDisposable()

    init {
        transactionService.transactionStatusObservable
                .observeOn(Schedulers.io())
                .subscribe {
                    syncState()
                }
                .let {
                    disposables.add(it)
                }

        syncTransactionData(amount)
    }

    private fun syncTransactionData(amount: BigInteger) {
        val erc20KitTransactionData = erc20Kit.buildApproveTransactionData(spenderAddress, amount)

        transactionService.transactionData = TransactionData(
                erc20KitTransactionData.to,
                erc20KitTransactionData.value,
                erc20KitTransactionData.input,
        )
    }

    private fun syncState() {
        val amount = amount
        if (amount == null) {
            state = State.ApproveNotAllowed(listOf())
            return
        }

        val errors = mutableListOf<Throwable>()
        var loading = false

        if (allowance >= amount && amount > BigInteger.ZERO) { // 0 amount is used for USDT to drop existing allowance
            errors.add(TransactionAmountError.AlreadyApproved)
        }

        when (val transactionStatus = transactionService.transactionStatus) {
            DataState.Loading -> {
                loading = true
            }
            is DataState.Error -> {
                errors.add(transactionStatus.error)
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                if (transaction.totalAmount > ethereumBalance) {
                    errors.add(TransactionEthereumAmountError.InsufficientBalance(transaction.totalAmount))
                }
            }
        }

        state = when {
            errors.isEmpty() && !loading -> {
                State.ApproveAllowed
            }
            else -> {
                State.ApproveNotAllowed(errors)
            }
        }
    }

    override fun approve() {
        val transactionStatus = transactionService.transactionStatus
        if (transactionStatus !is DataState.Success) return

        val transaction = transactionStatus.data

        amount?.let { amount ->
            state = State.Loading

            val transactionData = erc20Kit.buildApproveTransactionData(spenderAddress, amount)
            ethereumKit.send(transactionData, transaction.gasData.gasPrice, transaction.gasData.gasLimit)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        state = State.Success
                    }, {
                        state = State.Error(it)
                    })
                    .let {
                        disposables.add(it)
                    }
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    sealed class State {
        class ApproveNotAllowed(val errors: List<Throwable>) : State()
        object ApproveAllowed : State()
        object Loading : State()
        object Success : State()
        class Error(val e: Throwable) : State()
    }

    sealed class TransactionAmountError : Exception() {
        object AlreadyApproved : TransactionAmountError()
    }

    sealed class TransactionEthereumAmountError : Exception() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionEthereumAmountError()
    }
}
