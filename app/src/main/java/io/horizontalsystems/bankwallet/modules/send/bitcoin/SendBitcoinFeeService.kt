package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendBitcoinFeeService(private val adapter: ISendBitcoinAdapter) {
    private val _bitcoinFeeInfoFlow = MutableStateFlow<BitcoinFeeInfo?>(null)
    val bitcoinFeeInfoFlow = _bitcoinFeeInfoFlow.asStateFlow()

    private var bitcoinFeeInfo: BitcoinFeeInfo? = null
    private var customUnspentOutputs: List<UnspentOutputInfo>? = null

    private var amount: BigDecimal? = null
    private var validAddress: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var feeRate: Int? = null

    private fun refreshFeeInfo() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        bitcoinFeeInfo = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.bitcoinFeeInfo(tmpAmount, tmpFeeRate, validAddress?.hex, customUnspentOutputs, pluginData)
        }
    }

    private fun emitState() {
        _bitcoinFeeInfoFlow.update { bitcoinFeeInfo }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFeeInfo()
        emitState()
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshFeeInfo()
        emitState()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        refreshFeeInfo()
        emitState()
    }

    fun setFeeRate(feeRate: Int?) {
        this.feeRate = feeRate

        refreshFeeInfo()
        emitState()
    }

    fun setCustomUnspentOutputs(customUnspentOutputs: List<UnspentOutputInfo>?) {
        this.customUnspentOutputs = customUnspentOutputs
        refreshFeeInfo()
        emitState()
    }

}
