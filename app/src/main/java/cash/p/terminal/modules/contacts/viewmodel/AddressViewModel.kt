package cash.p.terminal.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.order
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.address.*
import cash.p.terminal.modules.contacts.ContactAddressParser
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
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
                BlockchainType.BinanceChain
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
        val udnHandler = AddressHandlerUdn(TokenQuery(blockchain.type, TokenType.Native), "")
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
            BlockchainType.EthereumGoerli,
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
            is BlockchainType.Unsupported -> {
            }
        }

        return ContactAddressParser(domainAddressHandlers, rawAddressHandlers)
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