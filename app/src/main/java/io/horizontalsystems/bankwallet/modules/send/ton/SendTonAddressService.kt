package io.horizontalsystems.bankwallet.modules.send.ton

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.tonkit.FriendlyAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendTonAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var tonAddress: FriendlyAddress? = null

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
            tonAddress = FriendlyAddress.parse(address.hex)
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
        val tonAddress: FriendlyAddress?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
