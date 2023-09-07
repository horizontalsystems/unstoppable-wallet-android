package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.hodler.HodlerPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendBitcoinAddressService(private val adapter: ISendBitcoinAdapter, val predefinedAddress: String?) {

    private var address: Address? = predefinedAddress?.let { Address(it) }
    private var validAddress: Address? = predefinedAddress?.let { Address(it) }
    private var addressError: Throwable? = null

    private var pluginData: Map<Byte, IPluginData>? = null

    private val _stateFlow = MutableStateFlow(
        State(
            validAddress = validAddress,
            addressError = addressError,
            canBeSend = validAddress != null
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

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
            addressError = getError(e)
        }
    }

    private fun getError(error: Throwable): Throwable {
        val message = when (error) {
            is HodlerPlugin.UnsupportedAddressType -> Translator.getString(R.string.Send_Error_UnsupportedAddress)
            is AddressFormatException -> Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
            else -> error.message ?: error.javaClass.simpleName
        }

        return Throwable(message)
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                validAddress = validAddress,
                addressError = addressError,
                canBeSend = validAddress != null
            )
        }
    }

    data class State(
        val validAddress: Address?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
