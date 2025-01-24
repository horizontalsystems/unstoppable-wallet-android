package io.horizontalsystems.bankwallet.modules.send.address

import android.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Address

class EnterAddressViewModel : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var canBeSendToAddress: Boolean = false

    override fun createState() = EnterAddressUiState(
        address = address,
        addressError = addressError,
        canBeSendToAddress = canBeSendToAddress
    )

    fun onEnterAddress(address: Address?) {
        Log.e("AAA", "onEnterAddress: $address")
//        TODO("Not yet implemented")
    }
}

data class EnterAddressUiState(
    val address: Address?,
    val addressError: Throwable?,
    val canBeSendToAddress: Boolean,
)
