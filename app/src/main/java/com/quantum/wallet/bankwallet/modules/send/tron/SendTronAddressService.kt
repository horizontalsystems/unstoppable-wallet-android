package com.quantum.wallet.bankwallet.modules.send.tron

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.ISendTronAdapter
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronAddressService(private val adapter: ISendTronAdapter, private val token: Token) {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var tronAddress: TronAddress? = null
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
                addressError = Throwable(Translator.getString(R.string.Tron_SelfSendTrxNotAllowed))
            }

            tronAddress = validAddress
        } catch (e: Exception) {
            isInactiveAddress = false
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
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
