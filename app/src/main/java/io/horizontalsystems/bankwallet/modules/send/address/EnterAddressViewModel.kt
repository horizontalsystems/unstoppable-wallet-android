package io.horizontalsystems.bankwallet.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressSecurityCheckerChain
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBase58
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBech32
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBinanceChain
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBitcoinCash
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEvm
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerSolana
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTon
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTron
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerZcash
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

class EnterAddressViewModel(
    private val blockchainType: BlockchainType,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain,
    initialAddress: String?,
    private val amount: BigDecimal?,
    contactsRepository: ContactsRepository,
    recentAddressManager: RecentAddressManager,
    private val addressSecurityCheckerChain: AddressSecurityCheckerChain
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private val canBeSendToAddress: Boolean
        get() = inputState is DataState.Success
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

        if (value.isBlank()) {
            this.value = ""
            emitState()
        } else {
            try {
                inputState = DataState.Loading
                val addressString = addressExtractor.extractAddressFromUri(value.trim())
                this.value = addressString
                emitState()

                runAddressParser(addressString)
            } catch (e: Throwable) {
                inputState = DataState.Error(e)
                emitState()
            }
        }
    }

    private fun runAddressParser(addressText: String) {
        parseAddressJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val address = parseAddress(addressText)
                this@EnterAddressViewModel.address = address
                inputState = DataState.Success(address)

                addressFormatCheck = AddressCheckData(false, AddressCheckResult.Correct)
                phishingCheck = AddressCheckData(true)
                blacklistCheck = AddressCheckData(true)

                emitState()

                checkAddress(address)
            } catch (e: Throwable) {
                inputState = DataState.Error(e)
            }

            ensureActive()
            emitState()
        }
    }

    private suspend fun checkAddress(address: Address) {
        val issues = addressSecurityCheckerChain.handle(address)
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
    }

    private fun parseAddress(addressText: String): Address {
        val handler = addressParserChain.supportedHandler(addressText)
            ?: throw AddressValidationException.Unsupported()

        return handler.parseAddress(addressText)
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

            when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    val network = MainNet()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.BitcoinCash -> {
                    val network = MainNetBitcoinCash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.ECash -> {
                    val network = MainNetECash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.Litecoin -> {
                    val network = MainNetLitecoin()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.Dash -> {
                    val network = MainNetDash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                }

                BlockchainType.BinanceChain -> {
                    addressParserChain.addHandler(AddressHandlerBinanceChain())
                }

                BlockchainType.Zcash -> {
                    addressParserChain.addHandler(AddressHandlerZcash())
                }

                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(blockchainType))
                }

                BlockchainType.Solana -> {
                    addressParserChain.addHandler(AddressHandlerSolana())
                }

                BlockchainType.Tron -> {
                    addressParserChain.addHandler(AddressHandlerTron())
                }

                BlockchainType.Ton -> {
                    addressParserChain.addHandler(AddressHandlerTon())
                }

                is BlockchainType.Unsupported -> Unit
            }
            val addressUriParser = AddressUriParser(wallet.token.blockchainType, wallet.token.type)
            val recentAddressManager = RecentAddressManager(App.accountManager, App.appDatabase.recentAddressDao())
            val addressSecurityCheckerChain = App.addressSecurityCheckerChainFactory.securityCheckerChain(wallet.token.blockchainType)
            return EnterAddressViewModel(
                wallet.token.blockchainType,
                addressUriParser,
                addressParserChain,
                address,
                amount,
                App.contactsRepository,
                recentAddressManager,
                addressSecurityCheckerChain
            ) as T
        }
    }
}

data class EnterAddressUiState(
    val addressError: Throwable?,
    val canBeSendToAddress: Boolean,
    val recentAddress: String?,
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