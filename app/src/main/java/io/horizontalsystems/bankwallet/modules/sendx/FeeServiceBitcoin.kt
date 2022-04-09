package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import java.math.BigDecimal

class FeeServiceBitcoin(private val adapter: ISendBitcoinAdapter) {
    var fee: BigDecimal? = null
        private set

    private var amount: BigDecimal? = null
    private var validAddress: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var feeRate: Long? = null

    fun init(amount: BigDecimal?, validAddress: Address?, pluginData: Map<Byte, IPluginData>?) {
        this.amount = amount
        this.validAddress = validAddress
        this.pluginData = pluginData

        refreshFee()
    }

    private fun refreshFee() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        fee = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.fee(tmpAmount, tmpFeeRate, validAddress?.hex, pluginData)
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFee()
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshFee()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        refreshFee()
    }

    fun setFeeRate(feeRate: Long?) {
        this.feeRate = feeRate

        refreshFee()
    }

}
