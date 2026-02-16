package io.horizontalsystems.bankwallet.modules.send.zcash

import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.entities.Address
import java.math.BigDecimal

class SendZcashFeeService(
    private val adapter: ISendZcashAdapter
) : ServiceState<SendZcashFeeService.State>() {

    private var memo: String? = null
    private var address: Address? = null
    private var amount: BigDecimal? = null
    private var fee: BigDecimal? = null

    override fun createState() = State(
        fee = fee
    )

    suspend fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshAndEmit()
    }

    suspend fun setAddress(address: Address?) {
        this.address = address

        refreshAndEmit()
    }

    suspend fun setMemo(memo: String) {
        this.memo = memo

        refreshAndEmit()
    }

    private suspend fun refreshAndEmit() {
        val tmpAmount = amount
        val tmpAddress = address
        val tmpMemo = memo

        fee = if (tmpAmount == null || tmpAddress == null || tmpMemo == null) {
            null
        } else {
            adapter.fee(tmpAmount, tmpAddress.hex, tmpMemo)
        }

        emitState()
    }

    data class State(val fee: BigDecimal?)
}
