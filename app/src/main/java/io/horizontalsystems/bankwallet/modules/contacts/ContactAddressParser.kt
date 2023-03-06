package io.horizontalsystems.bankwallet.modules.contacts

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.address.IAddressHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactAddressParser(
    private val handlers: List<IAddressHandler>
) {
    suspend fun parseAddress(value: String): Address = withContext(Dispatchers.IO) {
        if (value.isBlank()) throw AddressValidationException.Blank()

        val vTrimmed = value.trim()

        val handler = handlers.firstOrNull {
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