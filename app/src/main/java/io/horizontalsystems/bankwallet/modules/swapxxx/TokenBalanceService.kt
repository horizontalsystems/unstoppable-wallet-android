package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class TokenBalanceService(private val adapterManager: IAdapterManager) {
    private var token: Token? = null
    private var balance: BigDecimal? = null

    private val _balanceFlow: MutableStateFlow<BigDecimal?> = MutableStateFlow(null)
    val balanceFlow = _balanceFlow.asStateFlow()

    fun setToken(token: Token?) {
        this.token = token

        refreshAvailableBalance()
        emitState()
    }

    private fun emitState() {
        _balanceFlow.update { balance }
    }

    private fun refreshAvailableBalance() {
        balance = token?.let {
            (adapterManager.getAdapterForToken(it) as? IBalanceAdapter)?.balanceData?.available
        }
    }

}