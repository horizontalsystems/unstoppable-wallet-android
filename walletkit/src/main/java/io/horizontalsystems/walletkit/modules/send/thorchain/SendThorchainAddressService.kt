package io.horizontalsystems.walletkit.modules.send.thorchain

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.thorchainkit.models.Address as ThorchainAddress
import io.horizontalsystems.thorchainkit.network.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendThorchainAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            addressError = addressError,
            canBeSend = addressError == null,
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
        val address = this.address ?: return

        try {
            ThorchainAddress.fromString(address.hex, Network.Mainnet)
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                addressError = addressError,
                canBeSend = addressError == null
            )
        }
    }

    data class State(
        val address: Address?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    ) {
        val validAddress: Address?
            get() = if (addressError == null) address else null
    }
}
