package io.horizontalsystems.core.modules.send.stellar

import io.horizontalsystems.core.core.ISendStellarAdapter
import io.horizontalsystems.core.core.ServiceState
import io.horizontalsystems.core.entities.Address
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
