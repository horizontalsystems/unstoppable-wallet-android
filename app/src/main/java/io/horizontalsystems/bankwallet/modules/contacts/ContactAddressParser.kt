package io.horizontalsystems.bankwallet.modules.contacts

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.address.IAddressHandler
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactAddressParser(
    private val domainAddressHandlers: List<IAddressHandler>,
    private val rawAddressHandlers: List<IAddressHandler>,
    private val blockchain: Blockchain
) {
    suspend fun parseAddress(value: String): Address = withContext(Dispatchers.IO) {
        try {
            val resolvedAddress = parse(value, domainAddressHandlers)
            parse(resolvedAddress.hex, rawAddressHandlers)
        } catch (error: Throwable) {
            parse(value, rawAddressHandlers)
        }
    }

    private fun parse(value: String, handlers: List<IAddressHandler>): Address {
        if (value.isBlank()) throw AddressValidationException.Blank()

        val vTrimmed = value.trim()

        val handler = handlers.firstOrNull {
            try {
                it.isSupported(vTrimmed)
            } catch (t: Throwable) {
                false
            }
        } ?: throw AddressValidationException.Unsupported(blockchain.name)

        try {
            return handler.parseAddress(vTrimmed)
        } catch (t: Throwable) {
            throw AddressValidationException.Invalid(t, blockchain.name)
        }
    }
}