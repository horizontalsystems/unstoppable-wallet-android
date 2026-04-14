package com.quantum.wallet.bankwallet.modules.send.stellar

import com.quantum.wallet.bankwallet.core.ISendStellarAdapter
import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.entities.Address
import java.math.BigDecimal

class SendStellarMinimumAmountService(
    private val adapter: ISendStellarAdapter
) : ServiceState<SendStellarMinimumAmountService.State>() {

    private var minimumAmount: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = State(
        minimumAmount = minimumAmount,
        error = error,
        canBeSend = error == null
    )

    suspend fun setValidAddress(address: Address?) {
        error = null

        try {
            minimumAmount = address?.let {
                adapter.getMinimumSendAmount(it.hex)
            }
        } catch (e: Throwable) {
            minimumAmount = null
            error = e
        }

        emitState()
    }

    data class State(
        val minimumAmount: BigDecimal?,
        val error: Throwable?,
        val canBeSend: Boolean,
    )

}
