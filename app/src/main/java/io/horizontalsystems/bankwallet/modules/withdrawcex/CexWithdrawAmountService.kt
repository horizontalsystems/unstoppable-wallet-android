package io.horizontalsystems.bankwallet.modules.withdrawcex

import android.util.Log
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.providers.CexWithdrawNetwork
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode

class CexWithdrawAmountService(
    private val amountValidator: AmountValidator,
    private val coinCode: String,
    private val freeBalance: BigDecimal,
    private var network: CexWithdrawNetwork,
    private val decimals: Int,
) {
    private var amount: BigDecimal? = null
    private var minAmount: BigDecimal? = null
    private var maxAmount: BigDecimal? = null
    private var feeFromAmount: Boolean = false
    private var fee: BigDecimal = calculateFee(amount, feeFromAmount, network)
    private var availableBalance: BigDecimal = freeBalance
    private var amountCaution: HSCaution? = null

    private val feePercent get() = network.feePercent
    private val fixedFee get() = network.fixedFee

    private val _stateFlow = MutableStateFlow(
        State(
            amount = amount,
            fee = fee,
            feeFromAmount = feeFromAmount,
            amountCaution = amountCaution,
            availableBalance = availableBalance,
            canBeSend = false,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    init {
        setNetwork(network)
    }

    private fun emitState() {
        val tmpAmount = amount
        val tmpAmountCaution = amountCaution

        val canBeSend = tmpAmount != null
                && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())

        _stateFlow.update {
            State(
                amount = amount,
                fee = calculateFee(amount, feeFromAmount, network),
                amountCaution = amountCaution,
                feeFromAmount = feeFromAmount,
                availableBalance = availableBalance,
                canBeSend = canBeSend
            )
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        sync()
    }

    fun setNetwork(network: CexWithdrawNetwork) {
        this.network = network
        minAmount = network.minAmount
        maxAmount = if (network.maxAmount > BigDecimal.ZERO) maxAmount else null

        sync()
    }

    fun setFeeFromAmount(feeFromAmount: Boolean) {
        this.feeFromAmount = feeFromAmount

        sync()
    }

    private fun sync() {
        refreshFee()
        refreshAvailableBalance()

        validateAmount()

        emitState()
    }

    private fun refreshFee() {
        fee = calculateFee(amount, feeFromAmount, network)
        Log.e("e", "refreshFee: amount= $amount, decimals= $decimals, fee: $fee, feeScale: ${fee.scale()}")
    }

    private fun refreshAvailableBalance() {
        availableBalance = if (feeFromAmount) {
            freeBalance
        } else {
            val balance = (freeBalance - fixedFee).scaledDivide(BigDecimal(1) + feePercent.scaledDivide(BigDecimal(100)))
            balance.coerceAtLeast(BigDecimal.ZERO)
        }

        Log.e("e", "refreshAvailableBalance: fee=$fee, availableBalance=$availableBalance")
    }

    private fun validateAmount() {
        val minAmount = if (feeFromAmount) {
            maxOf(minAmount ?: BigDecimal.ZERO, fee)
        } else {
            minAmount
        }

        Log.e("e", "validateAmount: minAmount=${this.minAmount}  calcMinAmount=$minAmount, maxAmount=$maxAmount")
        amountCaution = amountValidator.validate(amount, coinCode, availableBalance, minAmount?.stripTrailingZeros(), maxAmount?.stripTrailingZeros())
    }

    private fun BigDecimal.scaledDivide(divisor: BigDecimal) =
        this.divide(divisor, decimals, RoundingMode.HALF_UP)

    private fun calculateFee(amount: BigDecimal?, feeFromAmount: Boolean, network: CexWithdrawNetwork): BigDecimal {
        val amount = amount ?: BigDecimal.ZERO
        val fee = if (feeFromAmount) {
            amount - (amount - fixedFee).scaledDivide(BigDecimal(1) + feePercent.scaledDivide(BigDecimal(100)))
        } else {
            amount * feePercent.scaledDivide(BigDecimal(100)) + fixedFee
        }
        return fee.coerceAtLeast(network.minFee).setScale(decimals, RoundingMode.HALF_UP)
    }

    data class State(
        val amount: BigDecimal?,
        val fee: BigDecimal,
        val feeFromAmount: Boolean,
        val amountCaution: HSCaution?,
        val availableBalance: BigDecimal,
        val canBeSend: Boolean
    )
}
