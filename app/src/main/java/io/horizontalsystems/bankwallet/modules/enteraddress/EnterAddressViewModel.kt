package io.horizontalsystems.bankwallet.modules.enteraddress

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
import io.horizontalsystems.bankwallet.core.managers.PaidActionSettingsManager
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.address.AddressExtractor
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressValidator
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
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
    private val domainParser: AddressParserChain,
    private val addressValidator: EnterAddressValidator,
    private val addressCheckManager: AddressCheckManager,
    private val allowNull: Boolean,
    paidActionSettingsManager: PaidActionSettingsManager,
    localStorage: ILocalStorage
) : ViewModelUiState<EnterAddressUiState>() {
    private val recentEnabled = localStorage.recentlySentEnabled
    private var address: Address? = null
    private val canBeSendToAddress: Boolean
        get() = (allowNull || address != null) && !addressValidationInProgress && addressValidationError == null
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
    private val addressCheckEnabled = paidActionSettingsManager.isActionEnabled(SecureSend)

    private val recentlySentAddress: String?
        get() {
            return if (recentEnabled) recentAddress else null
        }

    private val recentlySentContact: SContact?
        get() {
            return if (recentEnabled) {
                recentAddress?.let { recent ->
                    contactNameAddresses.find { it.contactAddress.address == recentAddress }
                        ?.let { SContact(it.name, recent) }
                }
            } else {
                null
            }
        }

    init {
        initialAddress?.let {
            onEnterAddress(initialAddress)
        }

        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                if (value.isNotEmpty()) {
                    parseAddressJob?.cancel()
                    processAddress(value)
                }
            }
        }
    }

    override fun createState() = EnterAddressUiState(
        canBeSendToAddress = canBeSendToAddress,
        recentAddress = recentlySentAddress,
        recentContact = recentlySentContact,
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
            addressValidationInProgress = false
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

                        for (type in availableCheckTypes) {
                            val checkResult = try {
                                if (addressCheckManager.isClear(type, address, token)) {
                                    AddressCheckResult.Clear
                                } else {
                                    AddressCheckResult.Detected
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                AddressCheckResult.NotAvailable
                            }

                            ensureActive()
                            checkResults += mapOf(type to AddressCheckData(false, checkResult))
                            emitState()

                            if (type == AddressCheckType.Phishing && checkResult == AddressCheckResult.Detected) {
                                break
                            }
                        }
                    } else {
                        checkResults = availableCheckTypes.associateWith { AddressCheckData(false, AddressCheckResult.NotAllowed) }
                        emitState()
                    }
                }

                addressValidationInProgress = false

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
        private val allowNull: Boolean,
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
                addressParserChain,
                addressValidator,
                addressCheckManager,
                allowNull,
                App.paidActionSettingsManager,
                App.localStorage
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
    val addressValidationError: Throwable?,
    val checkResults: Map<AddressCheckType, AddressCheckData>,
    val addressCheckEnabled: Boolean,
) {
    val risky = checkResults.any { result -> result.value.checkResult == AddressCheckResult.Detected }
}

data class AddressCheckData(
    val inProgress: Boolean,
    val checkResult: AddressCheckResult = AddressCheckResult.NotAvailable
)