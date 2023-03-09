package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressViewModel(
    val blockchainType: BlockchainType,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val addressHandlers = mutableListOf<IAddressHandler>()

    fun addAddressHandler(handler: IAddressHandler) {
        addressHandlers.add(handler)
    }

    fun hasContacts() =
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()

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
