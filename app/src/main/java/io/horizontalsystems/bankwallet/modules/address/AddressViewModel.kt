package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressViewModel : ViewModel() {

    private val addressHandlers = mutableListOf<IAddressHandler>()

    fun addAddressHandler(handler: IAddressHandler) {
        addressHandlers.add(handler)
    }

    @Throws(AddressValidationException::class)
    suspend fun parseAddress(value: String): Address = withContext(Dispatchers.IO) {
        if (value.isBlank()) throw AddressValidationException.Blank()

        val vTrimmed = value.trim()

        val handler = addressHandlers.firstOrNull {
            try {
                it.isSupported(vTrimmed)
            } catch (t: Throwable) {
                false
            }
        } ?: throw AddressValidationException.Unsupported()

        try {
            handler.parseAddress(vTrimmed)
        } catch (t: Throwable) {
            throw AddressValidationException.Invalid(t)
        }
    }
}
