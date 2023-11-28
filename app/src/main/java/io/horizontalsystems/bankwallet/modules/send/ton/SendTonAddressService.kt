package io.horizontalsystems.bankwallet.modules.send.ton

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendTonAddressService(prefilledAddress: String?) {
    private var address: Address? = prefilledAddress?.let { Address(it) }
    private var addressError: Throwable? = null
    var tonAddress: String? = prefilledAddress
        private set

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            tonAddress = tonAddress,
            addressError = addressError,
            canBeSend = tonAddress != null,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setAddress(address: Address?) {
        this.address = address

        validateAddress()

        emitState()
    }

    private fun validateAddress() {
        addressError = null
        tonAddress = null
        val address = this.address ?: return

        try {
            tonAddress = address.hex
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                tonAddress = tonAddress,
                addressError = addressError,
                canBeSend = tonAddress != null
            )
        }
    }

    data class State(
        val address: Address?,
        val tonAddress: String?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
