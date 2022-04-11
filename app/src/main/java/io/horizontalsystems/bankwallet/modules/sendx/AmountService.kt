package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class AmountService(
    private val adapter: ISendBitcoinAdapter,
    private val coinCode: String
) {
    private var amount: BigDecimal? = null
    private var amountCaution: HSCaution? = null

    private var minimumSendAmount: BigDecimal? = null
    private var maximumSendAmount: BigDecimal? = null
    private var availableBalance: BigDecimal = adapter.balanceData.available
    private var validAddress: Address? = null
    private var feeRate: Long? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private val _stateFlow = MutableStateFlow(
        State(
            amount = amount,
            amountCaution = amountCaution,
            availableBalance = availableBalance,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun start() {
        refreshMinimumSendAmount()
        refreshMaximumSendAmount()
        refreshAvailableBalance()

        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                amount = amount,
                amountCaution = amountCaution,
                availableBalance = availableBalance,
            )
        }
    }

    private fun refreshAvailableBalance() {
        availableBalance = feeRate?.let { adapter.availableBalance(it, validAddress?.hex, pluginData) } ?: adapter.balanceData.available
    }

    private fun refreshMaximumSendAmount() {
        maximumSendAmount = pluginData?.let { adapter.maximumSendAmount(it) }
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(validAddress?.hex)
    }

    private fun validateAmount() {
        val tmpCoinAmount = amount
        val tmpMinimumSendAmount = minimumSendAmount
        val tmpMaximumSendAmount = maximumSendAmount

        amountCaution = when {
            tmpCoinAmount == null -> null
            tmpCoinAmount == BigDecimal.ZERO -> null
            tmpCoinAmount > availableBalance -> {
                SendErrorInsufficientBalance(coinCode)
            }
            tmpMinimumSendAmount != null && tmpCoinAmount < tmpMinimumSendAmount -> {
                SendErrorMinimumSendAmount(tmpMinimumSendAmount)
            }
            tmpMaximumSendAmount != null && tmpCoinAmount < tmpMaximumSendAmount -> {
                SendErrorMaximumSendAmount(tmpMaximumSendAmount)
            }
            else -> null
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validateAmount()

        emitState()
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshAvailableBalance()
        refreshMinimumSendAmount()
        validateAmount()

        emitState()
    }

    fun setFeeRate(feeRate: Long?) {
        this.feeRate = feeRate

        refreshAvailableBalance()
        validateAmount()

        emitState()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        refreshAvailableBalance()
        refreshMaximumSendAmount()
        validateAmount()

        emitState()
    }

    data class State(
        val amount: BigDecimal?,
        val amountCaution: HSCaution?,
        val availableBalance: BigDecimal
    )
}
