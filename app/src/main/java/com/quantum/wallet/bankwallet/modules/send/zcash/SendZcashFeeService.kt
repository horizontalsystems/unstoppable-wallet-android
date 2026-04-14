package com.quantum.wallet.bankwallet.modules.send.zcash

import com.quantum.wallet.bankwallet.core.ISendZcashAdapter
import com.quantum.wallet.bankwallet.core.InsufficientBalance
import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.entities.Address
import java.math.BigDecimal

class SendZcashFeeService(
    private val adapter: ISendZcashAdapter,
    private val coinCode: String
) : ServiceState<SendZcashFeeService.State>() {

    private var memo: String? = null
    private var address: Address? = null
    private var amount: BigDecimal? = null
    private var fee: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = State(
        fee = fee,
        error = error
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

        error = null
        fee = null

        if (tmpAmount != null && tmpAddress != null && tmpMemo != null) {
            try {
                fee = adapter.fee(tmpAmount, tmpAddress.hex, tmpMemo)
            } catch (e: Throwable) {
                val message = e.message
                if (message != null && message.contains("Insufficient balance", ignoreCase = true)) {
                    error = InsufficientBalance(coinCode)
                } else {
                    error = e
                }
            }
        }

        emitState()
    }

    data class State(val fee: BigDecimal?, val error: Throwable?)
}
