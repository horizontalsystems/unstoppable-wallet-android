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
import io.horizontalsystems.subscriptions.core.AddressBlacklist
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
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
    // State properties
    private var address: Address? = null
    private var value = ""
    private var inputState: DataState<Address>? = null
    private var addressValidationInProgress: Boolean = false
    private var addressValidationError: Throwable? = null
    private var parseAddressJob: Job? = null

    // External data
    private val recentAddress = recentAddressManager.getRecentAddress(token.blockchainType)
    private val contactNameAddresses = contactsRepository.getContactAddressesByBlockchain(token.blockchainType)
    private val availableCheckTypes = addressCheckManager.availableCheckTypes(token)
    private val addressExtractor = AddressExtractor(token.blockchainType, addressUriParser)

    private var checkResults: Map<AddressCheckType, AddressCheckData> =
        availableCheckTypes.associateWith { AddressCheckData(true) }

    private val canBeSendToAddress: Boolean
        get() = address != null && !addressValidationInProgress && addressValidationError == null

    init {
        initialAddress?.let {
            onEnterAddress(initialAddress)
        }
    }

    override fun createState() = EnterAddressUiState(
        canBeSendToAddress = canBeSendToAddress,
        recentAddress = recentAddress,
        recentContact = createRecentContact(),
        contacts = contactNameAddresses.map { SContact(it.name, it.contactAddress.address) },
        value = value,
        inputState = inputState,
        address = address,
        amount = amount,
        addressValidationError = addressValidationError,
        checkResults = checkResults
    )

    private fun createRecentContact(): SContact? {
        return recentAddress?.let { recent ->
            contactNameAddresses.find { it.contactAddress.address == recentAddress }?.let {
                SContact(it.name, recent)
            }
        }
    }

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()
        resetAddressState()

        if (value.isBlank()) {
            handleBlankInput()
            return
        }

        updateBlacklistStatus(value)
        processInputValue(value)
    }

    private fun resetAddressState() {
        address = null
        inputState = null
        addressValidationInProgress = true
        addressValidationError = null
    }

    private fun handleBlankInput() {
        value = ""
        emitState()
    }

    private fun updateBlacklistStatus(value: String) {
        if (!UserSubscriptionManager.isActionAllowed(AddressBlacklist)) {
            if (value.isNotBlank()) {
                checkResults = availableCheckTypes.associateWith {
                    AddressCheckData(false, AddressCheckResult.NotAllowed)
                }
                emitState()
            }
        } else {
            checkResults = availableCheckTypes.associateWith { AddressCheckData(true) }
        }
    }

    private fun processInputValue(inputValue: String) {
        try {
            val addressString = addressExtractor.extractAddressFromUri(inputValue.trim())
            value = addressString
            emitState()

            processAddress(addressString)
        } catch (e: Throwable) {
            value = inputValue.trim()
            inputState = DataState.Error(e)
            emitState()
        }
    }

    private fun processAddress(addressText: String) {
        parseAddressJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val parsedAddress = parseDomain(addressText)
                validateAndUpdateAddress(parsedAddress)

                if (addressValidationError != null) {
                    resetCheckResults()
                    inputState = DataState.Error(addressValidationError!!)
                } else if (UserSubscriptionManager.isActionAllowed(AddressBlacklist)) {
                    performAddressChecks(parsedAddress)
                    updateInputState(parsedAddress)
                }
                ensureActive()
                emitState()
            } catch (_: CancellationException) {
                // Ignore cancellation
            } catch (e: Throwable) {
                addressValidationInProgress = false
                addressValidationError = e
                inputState = DataState.Error(e)
                emitState()
            }
        }
    }

    private suspend fun validateAndUpdateAddress(parsedAddress: Address) {
        try {
            addressValidator.validate(parsedAddress)

            address = parsedAddress
            addressValidationInProgress = false
            addressValidationError = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Throwable) {
            address = null
            addressValidationInProgress = false
            addressValidationError = e
        }

        emitState()
    }

    private fun resetCheckResults() {
        checkResults = availableCheckTypes.associateWith { AddressCheckData(false) }
    }

    private suspend fun performAddressChecks(parsedAddress: Address) {
        availableCheckTypes.forEach { type ->
            val checkResult = addressCheckManager.check(type, parsedAddress, token)
            checkResults = checkResults.toMutableMap().apply {
                this[type] = AddressCheckData(false, checkResult)
            }
        }
    }

    private fun updateInputState(parsedAddress: Address) {
        inputState = if (
            addressValidationError == null &&
            checkResults.none { it.value.checkResult == AddressCheckResult.Detected }
        )
            DataState.Success(parsedAddress)
        else
            DataState.Error(Exception())
    }

    private fun parseDomain(addressText: String): Address {
        return domainParser.supportedHandler(addressText)?.parseAddress(addressText)
            ?: Address(addressText)
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
    val addressValidationError: Throwable?,
    val checkResults: Map<AddressCheckType, AddressCheckData>
)

data class AddressCheckData(
    val inProgress: Boolean,
    val checkResult: AddressCheckResult = AddressCheckResult.NotAvailable
)