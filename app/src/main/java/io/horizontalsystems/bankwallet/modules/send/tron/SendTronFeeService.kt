package io.horizontalsystems.bankwallet.modules.send.tron

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronFeeService(private val adapter: ISendTronAdapter, private val feeToken: Token) : ServiceState<SendTronFeeService.State>() {
    private var amount: BigDecimal? = null
    private var tronAddress: TronAddress? = null
    private var contract: Contract? = null

    private var feeLimit: Long? = null
    private var fee: BigDecimal? = null
    private var activationFee: BigDecimal? = null
    private var resourcesConsumed: String? = null
    private var error: Throwable? = null

    suspend fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFees()
        emitState()
    }

    suspend fun setContract(contract: Contract) {
        this.contract = contract

        refreshFees()
        emitState()
    }

    suspend fun setAddress(address: Address?) {
        this.tronAddress = address?.let {
            TronAddress.fromBase58(it.hex)
        }

        refreshFees()
        emitState()
    }

    private suspend fun refreshFees() = withContext(Dispatchers.Default) {
        feeLimit = null
        fee = null
        activationFee = null
        resourcesConsumed = null
        error = null

        try {
            val contract = this@SendTronFeeService.contract
            val fees: List<Fee>

            if (contract != null) {
                fees = adapter.estimateFee(contract)
            } else {
                val amount = this@SendTronFeeService.amount ?: return@withContext
                val tronAddress = this@SendTronFeeService.tronAddress ?: return@withContext

                fees = adapter.estimateFee(amount, tronAddress)
            }

            feeLimit = (fees.find { it is Fee.Energy } as? Fee.Energy)?.feeInSuns

            var bandwidth: String? = null
            var energy: String? = null

            fees.forEach { fee ->
                when (fee) {
                    is Fee.AccountActivation -> {
                        activationFee =
                            fee.feeInSuns.toBigDecimal().movePointLeft(feeToken.decimals)
                    }

                    is Fee.Bandwidth -> {
                        bandwidth = "${fee.points} Bandwidth"
                    }

                    is Fee.Energy -> {
                        val formattedEnergy =
                            App.numberFormatter.formatNumberShort(fee.required.toBigDecimal(), 0)
                        energy = "$formattedEnergy Energy"
                    }
                }
            }

            resourcesConsumed = if (bandwidth != null) {
                bandwidth + (energy?.let { " \n + $it" } ?: "")
            } else {
                energy
            }

            val totalFee = fees.sumOf { it.feeInSuns }.toBigInteger()
            fee = totalFee.toBigDecimal().movePointLeft(feeToken.decimals)
        } catch (error: Throwable) {
            this@SendTronFeeService.error = error
        }
    }

    override fun createState() = State(
        feeLimit = feeLimit,
        fee = fee,
        activationFee = activationFee,
        resourcesConsumed = resourcesConsumed,
        error = error,
        canBeSend = feeLimit != null && error == null
    )

    data class State(
        val feeLimit: Long?,
        val fee: BigDecimal?,
        val activationFee: BigDecimal?,
        val resourcesConsumed: String?,
        val error: Throwable?,
        val canBeSend: Boolean
    )
}
