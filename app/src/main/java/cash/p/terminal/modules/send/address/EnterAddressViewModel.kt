package cash.p.terminal.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.factories.AddressValidatorFactory
import cash.p.terminal.core.managers.RecentAddressManager
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.address.AddressHandlerFactory
import cash.p.terminal.modules.address.AddressParserChain
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class EnterAddressViewModel(
    private val token: Token,
    addressUriParser: AddressUriParser,
    initialAddress: String?,
    contactsRepository: ContactsRepository,
    recentAddressManager: RecentAddressManager,
    private val domainParser: AddressParserChain,
    private val addressValidator: EnterAddressValidator,
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private val canBeSendToAddress: Boolean
        get() = address != null && !addressValidationInProgress && addressValidationError == null
    private var recentAddress: String? = recentAddressManager.getRecentAddress(token.blockchainType)
    private val contactNameAddresses =
        contactsRepository.getContactAddressesByBlockchain(token.blockchainType)
    private var addressValidationInProgress: Boolean = false
    private var addressValidationError: Throwable? = null
    private var value = ""
    private var inputState: DataState<Address>? = null
    private var parseAddressJob: Job? = null

    private val addressExtractor = AddressExtractor(token.blockchainType, addressUriParser)

    init {
        initialAddress?.let {
            onEnterAddress(initialAddress)
        }
    }

    override fun createState() = EnterAddressUiState(
        canBeSendToAddress = canBeSendToAddress,
        recentAddress = recentAddress,
        recentContact = recentAddress?.let { recent ->
            contactNameAddresses.find { it.contactAddress.address == recentAddress }
                ?.let { SContact(it.name, recent) }
        },
        contacts = contactNameAddresses.map { SContact(it.name, it.contactAddress.address) },
        value = value,
        inputState = inputState,
        address = address,
        addressValidationInProgress = addressValidationInProgress,
        addressValidationError = addressValidationError,
    )

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        address = null
        inputState = null
        addressValidationInProgress = true
        addressValidationError = null

        if (value.isBlank()) {
            this.value = ""
            emitState()
        } else {
            try {
                val addressString = addressExtractor.extractAddressFromUri(value.trim())
                this.value = addressString
                emitState()

                processAddress(addressString)
            } catch (e: Throwable) {
                inputState = DataState.Error(e)
                emitState()
            }
        }
    }

    private fun processAddress(addressText: String) {
        parseAddressJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val address = parseDomain(addressText)
                try {
                    addressValidator.validate(address)
                    ensureActive()

                    this@EnterAddressViewModel.address = address
                    addressValidationInProgress = false
                    addressValidationError = null
                    emitState()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Throwable) {
                    ensureActive()

                    this@EnterAddressViewModel.address = null
                    addressValidationInProgress = false
                    addressValidationError = e

                    emitState()
                }

                if (addressValidationError != null) {
                    emitState()
                }

                inputState = if (
                    addressValidationError == null
                )
                    DataState.Success(address)
                else
                    DataState.Error(Exception())

                emitState()
            } catch (_: CancellationException) {
            } catch (e: Throwable) {
                inputState = DataState.Error(e)

                ensureActive()
                emitState()
            }
        }
    }

    private fun parseDomain(addressText: String): Address {
        return domainParser.supportedHandler(addressText)?.parseAddress(addressText) ?: Address(
            addressText
        )
    }

    class Factory(
        private val token: Token,
        private val address: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = token.blockchainType
            val addressHandlerFactory = AddressHandlerFactory(App.appConfigProvider.udnApiKey)
            val addressParserChain = addressHandlerFactory.parserChain(blockchainType, withEns = true)
            val addressUriParser = AddressUriParser(token.blockchainType, token.type)
            val recentAddressManager =
                RecentAddressManager(
                    App.accountManager,
                    App.appDatabase.recentAddressDao(),
                )
            val addressValidator = AddressValidatorFactory.get(token)
            return EnterAddressViewModel(
                token = token,
                addressUriParser = addressUriParser,
                initialAddress = address,
                contactsRepository = App.contactsRepository,
                recentAddressManager = recentAddressManager,
                domainParser = addressParserChain,
                addressValidator = addressValidator,
            ) as T
        }
    }
}

data class EnterAddressUiState(
    val canBeSendToAddress: Boolean,
    val recentAddress: String?,
    val recentContact: SContact?,
    val contacts: List<SContact>,
    val value: String,
    val inputState: DataState<Address>?,
    val address: Address?,
    val addressValidationInProgress: Boolean,
    val addressValidationError: Throwable?
)
