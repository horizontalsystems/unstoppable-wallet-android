package cash.p.terminal.modules.send.tron

import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.ServiceState
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.network.CreatedTransaction
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import kotlin.collections.find
import kotlin.getValue
import kotlin.let
import kotlin.toBigDecimal
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronFeeService(private val adapter: ISendTronAdapter, private val feeToken: Token) : ServiceState<SendTronFeeService.State>() {
    private val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)

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

        resetFees()
        refreshFees()
        emitState()
    }

    suspend fun setContract(contract: Contract) {
        this.contract = contract

        resetFees()
        refreshFees()
        emitState()
    }

    suspend fun setCreatedTransaction(createdTransaction: CreatedTransaction) {
        resetFees()
        // Keep the original fee_limit for sending — estimation only affects displayed fee
        this.feeLimit = createdTransaction.raw_data.fee_limit
        refreshFees(createdTransaction)
        // Fall back to fee_limit if estimation failed, so fee balance check still works
        if (fee == null && feeLimit != null) {
            fee = feeLimit?.toBigDecimal()?.movePointLeft(feeToken.decimals)
        }
        emitState()
    }

    suspend fun setTronAddress(address: Address?) {
        this.tronAddress = address

        resetFees()
        refreshFees()
        emitState()
    }

    private suspend fun refreshFees(
        createdTransaction: CreatedTransaction? = null
    ) = withContext(Dispatchers.Default) {
        try {
            val currentContract = contract
            val fees: List<Fee> = when {
                createdTransaction != null -> adapter.estimateFee(createdTransaction)
                currentContract != null -> adapter.estimateFee(currentContract)
                else -> {
                    val amount = this@SendTronFeeService.amount ?: return@withContext
                    val tronAddress = this@SendTronFeeService.tronAddress ?: return@withContext
                    adapter.estimateFee(amount, tronAddress)
                }
            }

            if (createdTransaction == null) {
                feeLimit = (fees.find { it is Fee.Energy } as? Fee.Energy)?.feeInSuns
            }

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
                            numberFormatter.formatNumberShort(fee.required.toBigDecimal(), 0)
                        energy = "$formattedEnergy Energy"
                    }
                }
            }

            resourcesConsumed = if (bandwidth != null) {
                bandwidth + (energy?.let { " \n + $it" } ?: "")
            } else {
                energy
            }

            val totalFee = fees.sumOf { it.feeInSuns }
            fee = totalFee.toBigDecimal().movePointLeft(feeToken.decimals)
        } catch (error: Throwable) {
            this@SendTronFeeService.error = error
        }
    }

    private fun resetFees() {
        feeLimit = null
        fee = null
        activationFee = null
        resourcesConsumed = null
        error = null
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
