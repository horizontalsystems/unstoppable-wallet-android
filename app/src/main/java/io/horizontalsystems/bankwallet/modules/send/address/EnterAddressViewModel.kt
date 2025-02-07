package io.horizontalsystems.bankwallet.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressSecurityCheckerChain
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
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

class EnterAddressViewModel(
    blockchainType: BlockchainType,
    addressUriParser: AddressUriParser,
    private val domainParser: AddressParserChain,
    initialAddress: String?,
    private val amount: BigDecimal?,
    contactsRepository: ContactsRepository,
    recentAddressManager: RecentAddressManager,
    private val addressSecurityCheckerChain: AddressSecurityCheckerChain,
    private val addressValidator: EnterAddressValidator
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private val canBeSendToAddress: Boolean
        get() = inputState is DataState.Success && addressFormatCheck.validationResult is AddressCheckResult.Correct
    private var recentAddress: String? = recentAddressManager.getRecentAddress(blockchainType)
    private val contacts = contactsRepository.getContactsFiltered(blockchainType)
    private var addressFormatCheck: AddressCheckData = AddressCheckData(true, null)
    private var phishingCheck: AddressCheckData = AddressCheckData(true, null)
    private var blacklistCheck: AddressCheckData = AddressCheckData(true, null)

    private var value = ""
    private var inputState: DataState<Address>? = null
    private var parseAddressJob: Job? = null

    private val addressExtractor = AddressExtractor(blockchainType, addressUriParser)

    init {
        initialAddress?.let {
            onEnterAddress(initialAddress)
        }
    }

    override fun createState() = EnterAddressUiState(
        addressError = addressError,
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
        addressFormatCheck = addressFormatCheck,
        phishingCheck = phishingCheck,
        blacklistCheck = blacklistCheck
    )

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        address = null
        inputState = null
        addressFormatCheck = AddressCheckData(true)
        phishingCheck = AddressCheckData(true)
        blacklistCheck = AddressCheckData(true)

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
                val validationResult = addressValidator.validate(address)
                ensureActive()
                this@EnterAddressViewModel.address = address
                addressFormatCheck = AddressCheckData(false, validationResult)
                emitState()

                if (addressFormatCheck.validationResult !is AddressCheckResult.Correct) {
                    ensureActive()
                    phishingCheck = AddressCheckData(false)
                    blacklistCheck = AddressCheckData(false)
                    emitState()
                } else {
                    val issues = addressSecurityCheckerChain.handle(address)
                    ensureActive()
                    phishingCheck = if (issues.any { it is AddressSecurityCheckerChain.SecurityIssue.Spam }) {
                        AddressCheckData(false, AddressCheckResult.Detected)
                    } else {
                        AddressCheckData(false, AddressCheckResult.Clear)
                    }
                    blacklistCheck = if (issues.any { it is AddressSecurityCheckerChain.SecurityIssue.Sanctioned }) {
                        AddressCheckData(false, AddressCheckResult.Detected)
                    } else {
                        AddressCheckData(false, AddressCheckResult.Clear)
                    }
                    emitState()
                }

                inputState = if (addressFormatCheck.validationResult is AddressCheckResult.Correct)
                    DataState.Success(address)
                else
                    DataState.Error(Throwable(addressFormatCheck.validationResult?.description))

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
            val addressParserChain =
                AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))
            val addressUriParser = AddressUriParser(wallet.token.blockchainType, wallet.token.type)
            val recentAddressManager = RecentAddressManager(App.accountManager, App.appDatabase.recentAddressDao())
            val addressSecurityCheckerChain = App.addressSecurityCheckerChainFactory.securityCheckerChain(wallet.token.blockchainType)
            val addressValidator = AddressValidatorFactory.get(wallet)
            return EnterAddressViewModel(
                wallet.token.blockchainType,
                addressUriParser,
                addressParserChain,
                address,
                amount,
                App.contactsRepository,
                recentAddressManager,
                addressSecurityCheckerChain,
                addressValidator
            ) as T
        }
    }
}

data class EnterAddressUiState(
    val addressError: Throwable?,
    val canBeSendToAddress: Boolean,
    val recentAddress: String?,
    val recentContact: SContact?,
    val contacts: List<SContact>,
    val value: String,
    val inputState: DataState<Address>?,
    val address: Address?,
    val amount: BigDecimal?,
    val addressFormatCheck: AddressCheckData,
    val phishingCheck: AddressCheckData,
    val blacklistCheck: AddressCheckData
)

data class AddressCheckData(
    val inProgress: Boolean,
    val validationResult: AddressCheckResult? = null
)