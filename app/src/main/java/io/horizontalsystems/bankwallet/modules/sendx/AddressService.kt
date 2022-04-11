package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddressService(private val adapter: ISendBitcoinAdapter) {

    private var address: Address? = null
    private var validAddress: Address? = null
    private var addressError: Throwable? = null

    private var pluginData: Map<Byte, IPluginData>? = null

    private val _stateFlow = MutableStateFlow(
        State(
            validAddress = validAddress,
            addressError = addressError
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun start() {
        validateAddress()
        refreshValidAddress()

        emitState()
    }

    fun setAddress(address: Address?) {
        this.address = address

        validateAddress()
        refreshValidAddress()

        emitState()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        validateAddress()
        refreshValidAddress()

        emitState()
    }

    private fun refreshValidAddress() {
        validAddress = if (addressError == null) address else null
    }

    private fun validateAddress() {
        addressError = null
        val address = this.address ?: return

        try {
            adapter.validate(address.hex, pluginData)
        } catch (e: Exception) {
            addressError = e
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                validAddress = validAddress,
                addressError = addressError
            )
        }
    }

    data class State(val validAddress: Address?, val addressError: Throwable?)
}
