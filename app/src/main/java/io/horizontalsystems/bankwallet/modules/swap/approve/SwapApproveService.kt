package io.horizontalsystems.bankwallet.modules.swap.approve

import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigInteger

class SwapApproveService(
        private val erc20Kit: Erc20Kit,
        amount: BigInteger,
        private val spenderAddress: Address,
        private val allowance: BigInteger,
) {
    private val stateSubject = BehaviorSubject.createDefault<State>(State.ApproveNotAllowed(listOf()))
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)
    var state: State = State.ApproveNotAllowed(listOf())
        private set(value) {
            field = value

            stateSubject.onNext(value)
        }

    var amount: BigInteger? = amount
        set(value) {
            field = value
            syncState()
        }

    init {
        syncState()
    }

    private fun syncState() {
        val amount = amount
        if (amount == null) {
            state = State.ApproveNotAllowed(listOf())
            return
        }

        val errors = mutableListOf<Throwable>()

        if (allowance >= amount && amount > BigInteger.ZERO) { // 0 amount is used for USDT to drop existing allowance
            errors.add(TransactionAmountError.AlreadyApproved)
        }

        state = if (errors.isEmpty()) {
            val erc20KitTransactionData = erc20Kit.buildApproveTransactionData(spenderAddress, amount)

            State.ApproveAllowed(erc20KitTransactionData)
        } else {
            State.ApproveNotAllowed(errors)
        }
    }

    sealed class State {
        class ApproveNotAllowed(val errors: List<Throwable>) : State()
        class ApproveAllowed(val transactionData: TransactionData) : State()
    }

    sealed class TransactionAmountError : Exception() {
        object AlreadyApproved : TransactionAmountError()
    }

}
