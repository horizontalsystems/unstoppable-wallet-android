package io.horizontalsystems.bankwallet.modules.send.monero

import io.horizontalsystems.bankwallet.core.ISendMoneroAdapter
import io.horizontalsystems.bankwallet.entities.Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendMoneroFeeService(private val adapter: ISendMoneroAdapter) : AutoCloseable {
    private var memo: String? = null
    private var address: Address? = null
    private var amount: BigDecimal? = null

    private var fee: BigDecimal? = null
    private var inProgress = false
    private val _stateFlow = MutableStateFlow(
        State(
            fee = fee,
            inProgress = inProgress
        )
    )
    val stateFlow = _stateFlow.asStateFlow()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var estimateFeeJob: Job? = null

    private fun refreshFeeAndEmitState() {
        val amount = amount
        val address = address
        val memo = memo

        estimateFeeJob?.cancel()
        estimateFeeJob = coroutineScope.launch {
            if (amount != null && address != null) {
                inProgress = true
                emitState()

                delay(1000)
                ensureActive()
                try {
                    fee = adapter.estimateFee(amount, address.hex, memo)
                } catch (_: Throwable) {
                    delay(500)
                    refreshFeeAndEmitState()
                }
            } else {
                fee = null
            }

            inProgress = false
            emitState()
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFeeAndEmitState()
    }

    fun setAddress(address: Address?) {
        this.address = address

        refreshFeeAndEmitState()
    }

    fun setMemo(memo: String?) {
        this.memo = memo

        refreshFeeAndEmitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                fee = fee,
                inProgress = inProgress
            )
        }
    }

    data class State(
        val fee: BigDecimal?,
        val inProgress: Boolean
    )

    override fun close() {
        coroutineScope.cancel()
    }

}
