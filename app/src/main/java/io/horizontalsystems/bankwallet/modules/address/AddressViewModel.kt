package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.core.utils.AddressUriResult
import io.horizontalsystems.bankwallet.core.utils.ToncoinUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressViewModel(
    val blockchainType: BlockchainType,
    private val contactsRepository: ContactsRepository,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain,
    initial: Address?
) : ViewModel() {

    var address by mutableStateOf<Address?>(initial)
        private set
    var inputState by mutableStateOf<DataState<Address>?>(null)
        private set
    var value by mutableStateOf(initial?.hex ?: "")
        private set
    private var parseAddressJob: Job? = null

    init {
        initial?.hex?.let {
            parseAddress(it)
        }
    }

    fun hasContacts() =
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()

    fun parseText(value: String) {
        parseAddressJob?.cancel()

        if (value.isBlank()) {
            this.value = value
            inputState = null
            address = null
            return
        }

        val vTrimmed = value.trim()

        inputState = DataState.Loading

        parseForUri(vTrimmed)
    }

    private fun parseForUri(text: String) {
        if (blockchainType == BlockchainType.Ton && text.contains("//")) {
            ToncoinUriParser.getAddress(text)?.let { address ->
                parseAddress(address)
                return
            }
        }
        when (val result = addressUriParser.parse(text)) {
            is AddressUriResult.Uri -> {
                parseAddress(result.addressUri.address)
            }

            AddressUriResult.InvalidBlockchainType -> {
                inputState = DataState.Error(AddressValidationException.Invalid(Throwable("Invalid Blockchain Type"), blockchainType.title))
            }

            AddressUriResult.InvalidTokenType -> {
                inputState = DataState.Error(AddressValidationException.Invalid(Throwable("Invalid Token Type"), blockchainType.title))
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                parseAddress(text)
            }
        }
    }

    private fun parseAddress(addressText: String) {
        value = addressText
        parseAddressJob = viewModelScope.launch(Dispatchers.IO) {
            val handler = addressParserChain.supportedHandler(addressText) ?: run {
                ensureActive()
                withContext(Dispatchers.Main) {
                    inputState = DataState.Error(AddressValidationException.Unsupported())
                }
                return@launch
            }

            try {
                val parsedAddress = handler.parseAddress(addressText)
                ensureActive()
                withContext(Dispatchers.Main) {
                    address = parsedAddress
                    inputState = DataState.Success(parsedAddress)
                }
            } catch (t: Throwable) {
                ensureActive()
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
