package com.quantum.wallet.bankwallet.modules.send.solana

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.horizontalsystems.solanakit.models.Address as SolanaAddress

class SendSolanaAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null
    var solanaAddress: SolanaAddress? = null
        private set

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            solanaAddress = solanaAddress,
            addressError = addressError,
            canBeSend = solanaAddress != null,
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
        solanaAddress = null
        val address = this.address ?: return

        try {
            solanaAddress = SolanaAddress(address.hex)
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                solanaAddress = solanaAddress,
                addressError = addressError,
                canBeSend = solanaAddress != null
            )
        }
    }

    data class State(
        val address: Address?,
        val solanaAddress: SolanaAddress?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
