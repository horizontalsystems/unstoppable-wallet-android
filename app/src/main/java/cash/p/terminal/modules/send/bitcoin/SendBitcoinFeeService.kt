package cash.p.terminal.modules.send.bitcoin

import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinFeeInfo
import cash.p.terminal.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendBitcoinFeeService(private val adapter: ISendBitcoinAdapter) {
    private val _feeDataFlow = MutableStateFlow<BitcoinFeeInfo?>(null)
    val feeDataFlow = _feeDataFlow.asStateFlow()

    private var feeData: BitcoinFeeInfo? = null
    private var customUnspentOutputs: List<UnspentOutputInfo>? = null

    private var amount: BigDecimal? = null
    private var validAddress: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var feeRate: Int? = null

    private fun refreshFee() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        feeData = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.sendInfo(tmpAmount, tmpFeeRate, validAddress?.hex, customUnspentOutputs, pluginData)
        }
    }

    private fun emitState() {
        _feeDataFlow.update { feeData }
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

    fun setCustomUnspentOutputs(customUnspentOutputs: List<UnspentOutputInfo>?) {
        this.customUnspentOutputs = customUnspentOutputs
        refreshFee()
        emitState()
    }

}
