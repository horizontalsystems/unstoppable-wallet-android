package io.horizontalsystems.bankwallet.modules.send.ton

import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.tonkit.FriendlyAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendTonFeeService(private val adapter: ISendTonAdapter) {
    private var memo: String? = null
    private var address: FriendlyAddress? = null
    private var amount: BigDecimal? = null

    private var fee: BigDecimal? = null
    private val _stateFlow = MutableStateFlow(
        State(
            fee = fee
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private suspend fun refreshFee() {
        val amount = amount
        val address = address
        val memo = memo

        fee = if (amount != null && address != null) {
            adapter.estimateFee(amount, address, memo)
        } else {
            null
        }
    }

    suspend fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFee()
        emitState()
    }

    suspend fun setTonAddress(address: FriendlyAddress?) {
        this.address = address

        refreshFee()
        emitState()
    }

    suspend fun setMemo(memo: String?) {
        this.memo = memo

        refreshFee()
        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                fee = fee
            )
        }
    }


    data class State(
        val fee: BigDecimal?
    )
}
