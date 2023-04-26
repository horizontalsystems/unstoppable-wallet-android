package io.horizontalsystems.bankwallet.modules.swap.approve

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
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
    private val stateSubject = BehaviorSubject.createDefault<State>(State.ApproveNotAllowed())
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)
    var state: State = State.ApproveNotAllowed()
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
        state = when {
            amount == null -> {
                State.ApproveNotAllowed()
            }
            allowance >= amount && amount > BigInteger.ZERO -> {
                // 0 amount is used for USDT to drop existing allowance
                State.ApproveNotAllowed(TransactionAmountError.AlreadyApproved)
            }
            else -> {
                State.ApproveAllowed(erc20Kit.buildApproveTransactionData(spenderAddress, amount))
            }
        }
    }

    sealed class State {
        class ApproveNotAllowed(val error: Throwable? = null) : State()
        class ApproveAllowed(val transactionData: TransactionData) : State()
    }

    sealed class TransactionAmountError : Exception() {
        object AlreadyApproved : TransactionAmountError() {
            override fun getLocalizedMessage() =
                Translator.getString(R.string.Approve_Error_AlreadyApproved)
        }
    }

}
