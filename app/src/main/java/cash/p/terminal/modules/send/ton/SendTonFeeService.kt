package cash.p.terminal.modules.send.ton

import cash.p.terminal.core.ISendTonAdapter
import io.horizontalsystems.tonkit.FriendlyAddress
import io.tonapi.infrastructure.ClientException
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
import java.io.Closeable
import java.math.BigDecimal

class SendTonFeeService(private val adapter: ISendTonAdapter) : Closeable {
    private var memo: String? = null
    private var address: FriendlyAddress? = null
    private var amount: BigDecimal? = null

    private var fee: FeeStatus? = null
    private var inProgress = false
    private val _stateFlow = MutableStateFlow(
        State(
            feeStatus = fee,
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
                    fee = FeeStatus.Success(adapter.estimateFee(amount, address, memo))
                } catch (e: Throwable) {
                    if (e is ClientException) {
                        fee = FeeStatus.NoEnoughBalance
                    }
                    e.printStackTrace()
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

    fun setTonAddress(address: FriendlyAddress?) {
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
                feeStatus = fee,
                inProgress = inProgress
            )
        }
    }


    data class State(
        val feeStatus: FeeStatus?,
        val inProgress: Boolean
    )

    override fun close() {
        coroutineScope.cancel()
    }
}
