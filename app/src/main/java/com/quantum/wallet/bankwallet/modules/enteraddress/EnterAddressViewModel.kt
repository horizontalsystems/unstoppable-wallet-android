package com.quantum.wallet.bankwallet.modules.enteraddress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.address.AddressCheckManager
import com.quantum.wallet.bankwallet.core.address.AddressCheckResult
import com.quantum.wallet.bankwallet.core.address.AddressCheckType
import com.quantum.wallet.bankwallet.core.factories.AddressValidatorFactory
import com.quantum.wallet.bankwallet.core.managers.ActionCompletedDelegate
import com.quantum.wallet.bankwallet.core.managers.RecentAddressManager
import com.quantum.wallet.bankwallet.core.utils.AddressUriParser
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.DataState
import com.quantum.wallet.bankwallet.modules.address.AddressHandlerEns
import com.quantum.wallet.bankwallet.modules.address.AddressHandlerUdn
import com.quantum.wallet.bankwallet.modules.address.AddressParserChain
import com.quantum.wallet.bankwallet.modules.address.EnsResolverHolder
import com.quantum.wallet.bankwallet.modules.contacts.ContactsRepository
import com.quantum.wallet.bankwallet.modules.send.address.AddressExtractor
import com.quantum.wallet.bankwallet.modules.send.address.EnterAddressValidator
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import com.quantum.wallet.subscriptions.core.ScamProtection
import com.quantum.wallet.subscriptions.core.SecureSend
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.drop
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
    private val localStorage: ILocalStorage
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
    private val checkJobs: MutableMap<AddressCheckType, Job> = mutableMapOf()
    private var hasPremium = UserSubscriptionManager.isActionAllowed(SecureSend)

    private val addressExtractor = AddressExtractor(token.blockchainType, addressUriParser)
    private val addressCheckEnabled: Boolean
        get() = hasPremium && AddressCheckType.entries.any { it.name in localStorage.enabledPaidActions }

    private val recentlySentAddress = if (recentEnabled) recentAddress else null

    private val recentlySentContact: SContact? = if (recentEnabled) {
        recentAddress?.let { recent ->
            contactNameAddresses.find { it.contactAddress.address == recentAddress }
                ?.let { SContact(it.name, recent) }
        }
    } else {
        null
    }

    init {
        initialAddress?.let { onEnterAddress(it) }
        observeSubscription()
        observeCheckSettings()
    }

    private fun cancelAllJobs() {
        parseAddressJob?.cancel()
        checkJobs.values.forEach { it.cancel() }
        checkJobs.clear()
    }

    private fun buildInitialCheckResults(): Map<AddressCheckType, AddressCheckData> =
        if (UserSubscriptionManager.isActionAllowed(ScamProtection)) {
            availableCheckTypes.associateWith { type ->
                if (isCheckEnabled(type)) AddressCheckData(inProgress = true, disabled = false)
                else AddressCheckData(inProgress = false, disabled = true)
            }
        } else {
            availableCheckTypes.associateWith {
                AddressCheckData(inProgress = false, disabled = false, checkResult = AddressCheckResult.NotAllowed)
            }
        }

    private fun observeSubscription() {
        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect { subscription ->
                hasPremium = UserSubscriptionManager.isActionAllowed(SecureSend)
                if (value.isNotEmpty()) {
                    cancelAllJobs()
                    checkResults = buildInitialCheckResults()
                    addressValidationInProgress = true
                    emitState()
                    processAddress(value)
                }
            }
        }
    }

    private fun observeCheckSettings() {
        viewModelScope.launch {
            var previous = localStorage.enabledPaidActions
            localStorage.enabledPaidActionsFlow.drop(1).collect { enabledActions ->
                for (type in availableCheckTypes) {
                    val key = type.name
                    val wasEnabled = key in previous
                    val isEnabled = key in enabledActions
                    if (wasEnabled == isEnabled) continue
                    val currentAddress = address ?: continue
                    if (value.isEmpty() || addressValidationError != null) continue
                    if (!isEnabled) {
                        checkJobs[type]?.cancel()
                        checkResults += mapOf(type to AddressCheckData(inProgress = false, disabled = true))
                        inputState = if (checkResults.none { it.value.checkResult == AddressCheckResult.Detected })
                            DataState.Success(currentAddress)
                        else DataState.Error(Exception())
                        emitState()
                    } else {
                        checkResults += mapOf(type to AddressCheckData(inProgress = true, disabled = false))
                        addressValidationInProgress = true
                        emitState()
                        startCheckJob(type, currentAddress)
                    }
                }
                previous = enabledActions
            }
        }
    }

    private fun isCheckEnabled(type: AddressCheckType) = type.name in localStorage.enabledPaidActions

    private fun startCheckJob(type: AddressCheckType, address: Address) {
        checkJobs[type]?.cancel()
        checkJobs[type] = viewModelScope.launch(Dispatchers.Default) {
            val result = try {
                if (addressCheckManager.isClear(type, address, token)) AddressCheckResult.Clear
                else AddressCheckResult.Detected
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AddressCheckResult.NotAvailable
            }
            checkResults += mapOf(type to AddressCheckData(inProgress = false, disabled = false, checkResult = result))
            tryFinalizeProcessing()
        }
    }

    private fun tryFinalizeProcessing() {
        if (!addressValidationInProgress) return
        if (checkResults.any { it.value.inProgress }) return
        addressValidationInProgress = false
        inputState = if (
            addressValidationError == null &&
            checkResults.none { it.value.checkResult == AddressCheckResult.Detected }
        ) DataState.Success(address!!)
        else DataState.Error(Exception())
        emitState()
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
        hasPremium = hasPremium
    )

    fun onEnterAddress(value: String) {
        cancelAllJobs()

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
                checkResults = buildInitialCheckResults()
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
                val address = validateAddress(addressText) ?: return@launch
                dispatchChecks(address)
            } catch (_: CancellationException) {
            } catch (e: Throwable) {
                inputState = DataState.Error(e)
                ensureActive()
                emitState()
            }
        }
    }

    private suspend fun validateAddress(addressText: String): Address? {
        val address = domainParser.supportedHandler(addressText)
            ?.parseAddress(addressText) ?: Address(addressText)
        return try {
            addressValidator.validate(address)
            currentCoroutineContext().ensureActive()
            this.address = address
            addressValidationError = null
            emitState()
            address
        } catch (_: CancellationException) {
            throw CancellationException()
        } catch (e: Throwable) {
            currentCoroutineContext().ensureActive()
            this.address = null
            addressValidationInProgress = false
            addressValidationError = e
            checkResults = mapOf()
            inputState = DataState.Error(Exception())
            emitState()
            null
        }
    }

    private fun dispatchChecks(address: Address) {
        if (!addressCheckEnabled) {
            addressValidationInProgress = false
            inputState = DataState.Success(address)
            emitState()
            return
        }
        if (!UserSubscriptionManager.isActionAllowed(ScamProtection)) {
            checkResults = availableCheckTypes.associateWith {
                AddressCheckData(inProgress = false, disabled = false, checkResult = AddressCheckResult.NotAllowed)
            }
            addressValidationInProgress = false
            inputState = DataState.Success(address)
            emitState()
            return
        }
        for (type in availableCheckTypes) {
            if (isCheckEnabled(type)) startCheckJob(type, address)
            else {
                checkResults += mapOf(type to AddressCheckData(inProgress = false, disabled = true))
                emitState()
            }
        }
        tryFinalizeProcessing()
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
    val hasPremium: Boolean,
) {
    val risky = checkResults.any { result -> result.value.checkResult == AddressCheckResult.Detected }
}

data class AddressCheckData(
    val inProgress: Boolean,
    val disabled: Boolean,
    val checkResult: AddressCheckResult = AddressCheckResult.NotAvailable
)