package cash.p.terminal.modules.send.tron

import cash.p.terminal.R
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.entities.Address
import cash.p.terminal.ui.compose.components.FormsInputStateWarning
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronAddressService(
    private val adapter: ISendTronAdapter,
    private val token: Token,
    prefilledAddress: String?
) {
    var address: Address? = prefilledAddress?.let { Address(it) }
        private set
    private var addressError: Throwable? = null
    private var tronAddress: TronAddress? = prefilledAddress?.let { TronAddress.fromBase58(it) }
    private var isInactiveAddress: Boolean = false

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            tronAddress = tronAddress,
            addressError = addressError,
            isInactiveAddress = isInactiveAddress,
            canBeSend = tronAddress != null && (addressError == null || addressError is FormsInputStateWarning)
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun setAddress(address: Address?) {
        this.address = address

        validateAddress()

        emitState()
    }

    private suspend fun validateAddress() {
        addressError = null
        tronAddress = null
        val address = this.address ?: return

        try {
            val validAddress = TronAddress.fromBase58(address.hex)
            isInactiveAddress = !adapter.isAddressActive(validAddress)

            if (token.type == TokenType.Native && adapter.isOwnAddress(validAddress)) {
                addressError = Throwable(cash.p.terminal.strings.helpers.Translator.getString(R.string.Tron_SelfSendTrxNotAllowed))
            }

            tronAddress = validAddress
        } catch (e: Exception) {
            isInactiveAddress = false
            addressError = Throwable(cash.p.terminal.strings.helpers.Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                tronAddress = tronAddress,
                addressError = addressError,
                isInactiveAddress = isInactiveAddress,
                canBeSend = tronAddress != null && (addressError == null || addressError is FormsInputStateWarning)
            )
        }
    }

    data class State(
        val address: Address?,
        val tronAddress: TronAddress?,
        val addressError: Throwable?,
        val isInactiveAddress: Boolean,
        val canBeSend: Boolean
    )
}
