package cash.p.terminal.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressViewModel(
    val blockchainType: BlockchainType,
    private val contactsRepository: ContactsRepository,
    initial: Address?
) : ViewModel() {

    var address by mutableStateOf<Address?>(initial)
        private set
    var inputState by mutableStateOf<DataState<Address>?>(null)
        private set
    var value by mutableStateOf(initial?.hex ?: "")
        private set
    private val addressHandlers = mutableListOf<IAddressHandler>()

    fun addAddressHandler(handler: IAddressHandler) {
        addressHandlers.add(handler)
    }

    fun hasContacts() =
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()

    fun parseAddressX(value: String) {
        this.value = value

        if (value.isBlank()) {
            inputState = null
            address = null
            return
        }

        val vTrimmed = value.trim()

        inputState = DataState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val handler = addressHandlers.firstOrNull {
                try {
                    it.isSupported(vTrimmed)
                } catch (t: Throwable) {
                    false
                }
            } ?: run {
                inputState = DataState.Error(AddressValidationException.Unsupported())
                return@launch
            }

            try {
                val parsedAddress = handler.parseAddress(vTrimmed)
                withContext(Dispatchers.Main) {
                    address = parsedAddress
                    inputState = DataState.Success(parsedAddress)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    inputState = DataState.Error(t)
                }
            }
        }
    }

    fun onAddressError(addressError: Throwable?) {
        addressError?.let {
            inputState = DataState.Error(addressError)
        }
    }
}
