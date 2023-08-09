package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendBitcoinFeeService(private val adapter: ISendBitcoinAdapter) {
    private val _feeFlow = MutableStateFlow<BigDecimal?>(null)
    val feeFlow = _feeFlow.asStateFlow()

    private var fee: BigDecimal? = null

    private var amount: BigDecimal? = null
    private var validAddress: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var feeRate: Int? = null

    private fun refreshFee() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        fee = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.fee(tmpAmount, tmpFeeRate, validAddress?.hex, pluginData)
        }
    }

    private fun emitState() {
        _feeFlow.update { fee }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFee()
        emitState()
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshFee()
        emitState()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        refreshFee()
        emitState()
    }

    fun setFeeRate(feeRate: Int?) {
        this.feeRate = feeRate

        refreshFee()
        emitState()
    }

}
