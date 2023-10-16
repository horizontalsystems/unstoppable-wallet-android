package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBase58
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBech32
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBinanceChain
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBitcoinCash
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEvm
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerPure
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerSolana
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTron
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.address.IAddressHandler
import io.horizontalsystems.bankwallet.modules.contacts.ContactAddressParser
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AddressViewModel(
    private val contactUid: String?,
    private val contactsRepository: ContactsRepository,
    private val udnApiKey: String,
    evmBlockchainManager: EvmBlockchainManager,
    marketKit: MarketKitWrapper,
    contactAddress: ContactAddress?,
    definedAddresses: List<ContactAddress>?
) : ViewModel() {

    private val title = if (contactAddress == null)
        TranslatableString.ResString(R.string.Contacts_AddAddress)
    else
        TranslatableString.PlainString(contactAddress.blockchain.name)
    private var address = contactAddress?.address ?: ""
    private val editingAddress = contactAddress
    private var addressState: DataState<Address>? = contactAddress?.address?.let { DataState.Success(Address(it)) }
    private val availableBlockchains: List<Blockchain>

    init {
        availableBlockchains = if (contactAddress == null) {
            val allBlockchainTypes = evmBlockchainManager.allBlockchainTypes + listOf(
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
                BlockchainType.Solana,
                BlockchainType.BinanceChain,
                BlockchainType.ECash,
                BlockchainType.Tron
            )
            val definedBlockchainTypes = definedAddresses?.map { it.blockchain.type } ?: listOf()
            val availableBlockchainUids = allBlockchainTypes.filter { !definedBlockchainTypes.contains(it) }.map { it.uid }

            marketKit.blockchains(availableBlockchainUids).sortedBy { it.type.order }
        } else {
            listOf()
        }
    }

    private var blockchain = contactAddress?.blockchain ?: availableBlockchains.first()
    private var addressParser: ContactAddressParser = addressParser(blockchain)

    var uiState by mutableStateOf(uiState())
        private set

    fun onEnterAddress(address: String) {
        this.address = address

        emitUiState()

        validateAddress(address)
    }

    fun onEnterBlockchain(blockchain: Blockchain) {
        this.blockchain = blockchain
        this.addressParser = addressParser(blockchain)

        emitUiState()

        validateAddress(address)
    }

    private var validationJob: Job? = null

    private fun validateAddress(address: String) {
        validationJob?.cancel()

        if (address.isEmpty()) {
            addressState = null
            emitUiState()
            return
        }

        validationJob = viewModelScope.launch {
            addressState = DataState.Loading
            emitUiState()

            addressState = try {
                val parsedAddress = addressParser.parseAddress(address)
                ensureActive()
                contactsRepository.validateAddress(contactUid, ContactAddress(blockchain, parsedAddress.hex))
                DataState.Success(parsedAddress)
            } catch (error: Throwable) {
                ensureActive()
                DataState.Error(error)
            }
            emitUiState()
        }
    }

    private fun addressParser(blockchain: Blockchain): ContactAddressParser {
        val udnHandler = AddressHandlerUdn(TokenQuery(blockchain.type, TokenType.Native), "", udnApiKey)
        val domainAddressHandlers = mutableListOf<IAddressHandler>(udnHandler)
        val rawAddressHandlers = mutableListOf<IAddressHandler>()

        when (blockchain.type) {
            BlockchainType.Bitcoin -> {
                val network = MainNet()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBech32(network))
            }
            BlockchainType.BitcoinCash -> {
                val network = MainNetBitcoinCash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBitcoinCash(network))
            }
            BlockchainType.ECash -> {
                val network = MainNetECash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBitcoinCash(network))
            }
            BlockchainType.Litecoin -> {
                val network = MainNetLitecoin()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBech32(network))
            }
            BlockchainType.Dash -> {
                val network = MainNetDash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
            }
            BlockchainType.BinanceChain -> {
                rawAddressHandlers.add(AddressHandlerBinanceChain())
            }
            BlockchainType.Zcash -> {
                //No validation
                rawAddressHandlers.add(AddressHandlerPure())
            }
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                domainAddressHandlers.add(AddressHandlerEns(EnsResolverHolder.resolver))
                rawAddressHandlers.add(AddressHandlerEvm())
            }
            BlockchainType.Solana -> {
                rawAddressHandlers.add(AddressHandlerSolana())
            }
            BlockchainType.Tron -> {
                rawAddressHandlers.add(AddressHandlerTron())
            }
            is BlockchainType.Unsupported -> {
            }
        }

        return ContactAddressParser(domainAddressHandlers, rawAddressHandlers, blockchain)
    }

    private fun uiState() = UiState(
        headerTitle = title,
        editingAddress = editingAddress,
        addressState = addressState,
        address = address,
        blockchain = blockchain,
        canChangeBlockchain = editingAddress == null,
        showDelete = editingAddress != null,
        availableBlockchains = availableBlockchains,
        doneEnabled = addressState is DataState.Success
    )

    private fun emitUiState() {
        uiState = uiState()
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val editingAddress: ContactAddress?,
        val addressState: DataState<Address>?,
        val address: String,
        val blockchain: Blockchain,
        val canChangeBlockchain: Boolean,
        val showDelete: Boolean,
        val availableBlockchains: List<Blockchain>,
        val doneEnabled: Boolean
    )
}
