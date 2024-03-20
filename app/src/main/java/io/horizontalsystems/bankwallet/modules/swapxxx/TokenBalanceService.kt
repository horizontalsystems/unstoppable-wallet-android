package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class TokenBalanceService(
    private val adapterManager: IAdapterManager,
) : ServiceState<TokenBalanceService.State>() {
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var balance: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = State(
        balance = balance,
        error = error
    )

    fun setToken(token: Token?) {
        this.token = token

        refreshAvailableBalance()
        validate()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validate()

        emitState()
    }

    private fun validate() {
        error = null

        val balance = balance ?: return
        val amount = amount ?: return

        if (amount > balance) {
            error = SwapMainModule.SwapError.InsufficientBalanceFrom
        }
    }

    private fun refreshAvailableBalance() {
        balance = token?.let {
            (adapterManager.getAdapterForToken(it) as? IBalanceAdapter)?.balanceData?.available
        }
    }

    data class State(val balance: BigDecimal?, val error: Throwable?)
}