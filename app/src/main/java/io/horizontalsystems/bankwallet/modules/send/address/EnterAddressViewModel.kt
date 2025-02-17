package io.horizontalsystems.bankwallet.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressCheckManager
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.factories.AddressValidatorFactory
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

class EnterAddressViewModel(
    private val token: Token,
    addressUriParser: AddressUriParser,
    private val domainParser: AddressParserChain,
    initialAddress: String?,
    private val amount: BigDecimal?,
    contactsRepository: ContactsRepository,
    recentAddressManager: RecentAddressManager,
    private val addressValidator: EnterAddressValidator,
    private val addressCheckManager: AddressCheckManager
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private val canBeSendToAddress: Boolean
        get() = address != null && !addressValidationInProgress && addressValidationError == null
    private var recentAddress: String? = recentAddressManager.getRecentAddress(token.blockchainType)
    private val contacts = contactsRepository.getContactsFiltered(token.blockchainType)
    private var addressValidationInProgress: Boolean = false
    private var addressValidationError: Throwable? = null
    private val availableCheckTypes = addressCheckManager.availableCheckTypes(token)
    private var checkResults: Map<AddressCheckType, AddressCheckData> =
        availableCheckTypes.associateWith { AddressCheckData(true) }.toMutableMap()
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
            contacts.find { contact -> contact.addresses.any { it.address == recentAddress } }?.let { SContact(it.name, recent) }
        },
        contacts = contacts.flatMap { contact -> contact.addresses.map { SContact(contact.name, it.address) } },
        value = value,
        inputState = inputState,
        address = address,
        amount = amount,
        addressValidationInProgress = addressValidationInProgress,
        addressValidationError = addressValidationError,
        checkResults = checkResults
    )

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        address = null
        inputState = null
        addressValidationInProgress = true
        addressValidationError = null
        checkResults = availableCheckTypes.associateWith { AddressCheckData(true) }

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
                    checkResults = availableCheckTypes.associateWith { AddressCheckData(false) }
                    emitState()
                } else {
                    availableCheckTypes.forEach { type ->
                        val checkResult = addressCheckManager.check(type, address, token)
                        ensureActive()
                        checkResults = checkResults.toMutableMap().apply {
                            this[type] = AddressCheckData(false, checkResult)
                        }
                        emitState()
                    }
                }

                inputState = if (
                    addressValidationError == null &&
                    checkResults.none { it.value.checkResult == AddressCheckResult.Detected }
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
        return domainParser.supportedHandler(addressText)?.parseAddress(addressText) ?: Address(addressText)
    }

    class Factory(
        private val wallet: Wallet,
        private val address: String?,
        private val amount: BigDecimal?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = wallet.token.blockchainType
            val coinCode = wallet.coin.code
            val tokenQuery = TokenQuery(blockchainType, wallet.token.type)
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))
            val addressUriParser = AddressUriParser(wallet.token.blockchainType, wallet.token.type)
            val recentAddressManager = RecentAddressManager(App.accountManager, App.appDatabase.recentAddressDao())
            val addressValidator = AddressValidatorFactory.get(wallet)
            val addressCheckManager = AddressCheckManager(App.spamManager, App.appConfigProvider, App.evmBlockchainManager, App.evmSyncSourceManager)
            return EnterAddressViewModel(
                wallet.token,
                addressUriParser,
                addressParserChain,
                address,
                amount,
                App.contactsRepository,
                recentAddressManager,
                addressValidator,
                addressCheckManager
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
    val amount: BigDecimal?,
    val addressValidationInProgress: Boolean,
    val addressValidationError: Throwable?,
    val checkResults: Map<AddressCheckType, AddressCheckData>
)

data class AddressCheckData(
    val inProgress: Boolean,
    val checkResult: AddressCheckResult = AddressCheckResult.NotAvailable
)