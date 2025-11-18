package io.horizontalsystems.bankwallet.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressCheckManager
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.factories.AddressValidatorFactory
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
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
    localStorage: ILocalStorage,
    addressCheckerSkippable: Boolean,
    private val domainParser: AddressParserChain,
    private val addressValidator: EnterAddressValidator,
    private val addressCheckManager: AddressCheckManager,
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private val canBeSendToAddress: Boolean
        get() = address != null && !addressValidationInProgress && addressValidationError == null
    private val recentAddress: String? = recentAddressManager.getRecentAddress(token.blockchainType)
    private val contactNameAddresses =
        contactsRepository.getContactAddressesByBlockchain(token.blockchainType)
            .sortedByDescending { it.contactAddress.address == recentAddress }
    private var addressValidationInProgress: Boolean = false
    private var addressValidationError: Throwable? = null
    private val availableCheckTypes = addressCheckManager.availableCheckTypes(token)
    private var checkResults: Map<AddressCheckType, AddressCheckData> = mapOf()
    private var value = ""
    private var inputState: DataState<Address>? = null
    private var parseAddressJob: Job? = null

    private val addressExtractor = AddressExtractor(token.blockchainType, addressUriParser)
    private val addressCheckEnabled = if (addressCheckerSkippable) {
        localStorage.recipientAddressCheckEnabled
    } else {
        true
    }

    init {
        initialAddress?.let {
            onEnterAddress(initialAddress)
        }
    }

    override fun createState() = EnterAddressUiState(
        canBeSendToAddress = canBeSendToAddress,
        contacts = contactNameAddresses.map { SContact(it.name, it.contactAddress.address) },
        value = value,
        inputState = inputState,
        address = address,
        addressValidationInProgress = addressValidationInProgress,
        addressValidationError = addressValidationError,
        checkResults = checkResults,
        addressCheckEnabled = addressCheckEnabled,
    )

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        address = null
        inputState = null
        addressValidationInProgress = true
        addressValidationError = null
        checkResults = mapOf()

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
                    checkResults = mapOf()
                    emitState()
                } else if (addressCheckEnabled) {
                    if (UserSubscriptionManager.isActionAllowed(ScamProtection)) {
                        checkResults = availableCheckTypes.associateWith { AddressCheckData(true) }
                        emitState()

                        availableCheckTypes.forEach { type ->
                            val checkResult = try {
                                if (addressCheckManager.isClear(type, address, token)) {
                                    AddressCheckResult.Clear
                                } else {
                                    AddressCheckResult.Detected
                                }
                            } catch (e: Throwable) {
                                AddressCheckResult.NotAvailable
                            }

                            checkResults += mapOf(type to AddressCheckData(false, checkResult))
                            ensureActive()
                            emitState()
                        }
                    } else {
                        checkResults = availableCheckTypes.associateWith { AddressCheckData(false, AddressCheckResult.NotAllowed) }
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
        return domainParser.supportedHandler(addressText)?.parseAddress(addressText) ?: Address(
            addressText
        )
    }

    class Factory(
        private val token: Token,
        private val address: String?,
        private val addressCheckerSkippable: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = token.blockchainType
            val coinCode = token.coin.code
            val tokenQuery = TokenQuery(blockchainType, token.type)
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val udnHandler =
                AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain =
                AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))
            val addressUriParser = AddressUriParser(token.blockchainType, token.type)
            val recentAddressManager =
                RecentAddressManager(
                    App.accountManager,
                    App.appDatabase.recentAddressDao(),
                    ActionCompletedDelegate
                )
            val addressValidator = AddressValidatorFactory.get(token)
            val addressCheckManager = AddressCheckManager(
                App.spamManager,
                App.appConfigProvider,
                App.evmBlockchainManager,
                App.evmSyncSourceManager
            )
            return EnterAddressViewModel(
                token,
                addressUriParser,
                address,
                App.contactsRepository,
                recentAddressManager,
                App.localStorage,
                addressCheckerSkippable,
                addressParserChain,
                addressValidator,
                addressCheckManager,
            ) as T
        }
    }
}

data class EnterAddressUiState(
    val canBeSendToAddress: Boolean,
    val contacts: List<SContact>,
    val value: String,
    val inputState: DataState<Address>?,
    val address: Address?,
    val addressValidationInProgress: Boolean,
    val addressValidationError: Throwable?,
    val checkResults: Map<AddressCheckType, AddressCheckData>,
    val addressCheckEnabled: Boolean,
)

data class AddressCheckData(
    val inProgress: Boolean,
    val checkResult: AddressCheckResult = AddressCheckResult.NotAvailable
)