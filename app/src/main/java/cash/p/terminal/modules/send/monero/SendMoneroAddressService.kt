package cash.p.terminal.modules.send.monero

import cash.p.terminal.R
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.entities.Address
import com.m2049r.xmrwallet.model.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendMoneroAddressService(prefilledAddress: String?) {
    private var address: Address? = prefilledAddress?.let { Address(it) }
    private var addressError: Throwable? = null

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            addressError = addressError,
            canBeSend = address != null,
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

        if (!Wallet.isAddressValid(address.hex)) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                addressError = addressError,
                canBeSend = address != null
            )
        }
    }

    data class State(
        val address: Address?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
