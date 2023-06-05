package cash.p.terminal.modules.send.tron

import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var tronAddress: TronAddress? = null

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            tronAddress = tronAddress,
            addressError = addressError,
            canBeSend = tronAddress != null,
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
        tronAddress = null
        val address = this.address ?: return

        try {
            tronAddress = TronAddress.fromBase58(address.hex)
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                tronAddress = tronAddress,
                addressError = addressError,
                canBeSend = tronAddress != null
            )
        }
    }

    data class State(
        val address: Address?,
        val tronAddress: TronAddress?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
