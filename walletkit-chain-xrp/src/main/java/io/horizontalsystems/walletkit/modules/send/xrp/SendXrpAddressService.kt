package io.horizontalsystems.walletkit.modules.send.xrp

import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendXrpAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var xAddressTag: Long? = null

    private val _stateFlow = MutableStateFlow(State(address, addressError, xAddressTag))
    val stateFlow = _stateFlow.asStateFlow()

    fun setAddress(address: Address?) {
        this.address = address
        validateAddress()
        emitState()
    }

    private fun validateAddress() {
        addressError = null
        xAddressTag = null
        val address = this.address ?: return

        try {
            XrpKit.validateAddress(address.hex)
            // an X-address carries its own destination tag; the user must not override it
            xAddressTag = XrpKit.decodeXAddress(address.hex)?.second
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update { State(address, addressError, xAddressTag) }
    }

    data class State(
        val address: Address?,
        val addressError: Throwable?,
        /** Tag packed in an X-address, or null for a classic address. */
        val xAddressTag: Long?,
    ) {
        val canBeSend: Boolean get() = addressError == null
        val validAddress: Address? get() = if (addressError == null) address else null
    }
}
