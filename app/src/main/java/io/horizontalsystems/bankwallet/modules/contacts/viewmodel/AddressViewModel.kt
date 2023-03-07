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
import io.horizontalsystems.bankwallet.modules.address.*
import io.horizontalsystems.bankwallet.modules.contacts.ContactAddressParser
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AddressViewModel(
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
    private var addressState: DataState<Address>? = contactAddress?.address?.let { DataState.Success(Address(it)) }
    private val canChangeBlockchain = contactAddress == null
    private val availableBlockchains: List<Blockchain>

    init {
        availableBlockchains = if (contactAddress == null) {
            val allBlockchainTypes = evmBlockchainManager.allBlockchainTypes + listOf(
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
                BlockchainType.Solana
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
        val handlers = mutableListOf<IAddressHandler>(udnHandler)

        when (blockchain.type) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.Zcash,
            BlockchainType.BinanceChain -> {
                // TODO add validators
            }
            BlockchainType.Ethereum,
            BlockchainType.EthereumGoerli,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.ArbitrumOne -> {
                val ensHandler = AddressHandlerEns(EnsResolverHolder.resolver)
                handlers.addAll(listOf(ensHandler, AddressHandlerEvm()))
            }
            BlockchainType.Solana -> {
                handlers.add(AddressHandlerSolana())
            }
            is BlockchainType.Unsupported -> {
            }
        }

        return ContactAddressParser(handlers)
    }

    private fun uiState() = UiState(
        headerTitle = title,
        addressState = addressState,
        address = address,
        blockchain = blockchain,
        canChangeBlockchain = canChangeBlockchain,
        availableBlockchains = availableBlockchains,
        doneEnabled = addressState is DataState.Success
    )

    private fun emitUiState() {
        uiState = uiState()
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val addressState: DataState<Address>?,
        val address: String,
        val blockchain: Blockchain,
        val canChangeBlockchain: Boolean,
        val availableBlockchains: List<Blockchain>,
        val doneEnabled: Boolean
    )
}
