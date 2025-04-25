package cash.p.terminal.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.contacts.ContactsRepository
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.launch

class AddressViewModel(
    val blockchainType: BlockchainType,
    private val contactsRepository: ContactsRepository,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain,
    initial: Address?
) : ViewModel() {

    private val fullAddressParserUseCase = FullAddressParserUseCase(
        blockchainType = blockchainType,
        addressUriParser = addressUriParser,
        addressParserChain = addressParserChain
    )

    val address = fullAddressParserUseCase.address
    val inputState = fullAddressParserUseCase.inputState
    val value = fullAddressParserUseCase.valueFlow

    fun hasContacts() =
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()

    init {
        initial?.hex?.let {
            viewModelScope.launch {
                fullAddressParserUseCase.parseAddress(it)
            }
        }
    }

    fun parseText(value: String) {
        viewModelScope.launch {
            fullAddressParserUseCase.parseText(value)
        }
    }

    fun onAddressError(addressError: Throwable?) =
        fullAddressParserUseCase.onAddressError(addressError)
}
