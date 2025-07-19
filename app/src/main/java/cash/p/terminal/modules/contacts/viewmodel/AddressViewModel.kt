package cash.p.terminal.modules.contacts.viewmodel

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.order
import cash.p.terminal.entities.Address
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.modules.address.AddressHandlerFactory
import cash.p.terminal.modules.address.AddressParserChain
import cash.p.terminal.modules.address.AddressValidationException
import cash.p.terminal.modules.address.IAddressHandler
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.strings.helpers.TranslatableString
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressViewModel(
    private val contactUid: String?,
    private val contactsRepository: ContactsRepository,
    private val addressHandlerFactory: AddressHandlerFactory,
    marketKit: MarketKitWrapper,
    contactAddress: ContactAddress?,
    definedAddresses: List<ContactAddress>?
) : ViewModelUiState<AddressViewModel.UiState>() {

    private val title = if (contactAddress == null)
        TranslatableString.ResString(R.string.Contacts_AddAddress)
    else
        TranslatableString.PlainString(contactAddress.blockchain.name)
    private var address = contactAddress?.address ?: ""
    private val editingAddress = contactAddress
    private var addressState: DataState<Address>? =
        contactAddress?.address?.let { DataState.Success(Address(it)) }
    private val availableBlockchains: List<Blockchain>

    init {
        availableBlockchains = if (contactAddress == null) {
            val allBlockchainTypes = EvmBlockchainManager.blockchainTypes + listOf(
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
                BlockchainType.Solana,
                BlockchainType.ECash,
                BlockchainType.Tron,
                BlockchainType.Ton,
                BlockchainType.Stellar,
            )
            val definedBlockchainTypes = definedAddresses?.map { it.blockchain.type } ?: listOf()
            val availableBlockchainUids =
                allBlockchainTypes.filter { !definedBlockchainTypes.contains(it) }.map { it.uid }

            marketKit.blockchains(availableBlockchainUids).sortedBy { it.type.order }
        } else {
            listOf()
        }
    }

    private var blockchain = contactAddress?.blockchain ?: availableBlockchains.first()
    private var addressParser: AddressParserChain =
        addressHandlerFactory.parserChain(blockchain.type, true)

    fun onEnterAddress(address: String) {
        this.address = address

        emitState()

        validateAddress(address)
    }

    fun onEnterBlockchain(blockchain: Blockchain) {
        this.blockchain = blockchain
        this.addressParser = addressHandlerFactory.parserChain(blockchain.type, true)

        emitState()

        validateAddress(address)
    }

    private var validationJob: Job? = null

    private fun validateAddress(address: String) {
        validationJob?.cancel()

        if (address.isEmpty()) {
            addressState = null
            emitState()
            return
        }

        validationJob = viewModelScope.launch {
            addressState = DataState.Loading
            emitState()

            addressState = try {
                val parsedAddress = parseAddress(addressParser, address.trim())
                ensureActive()
                contactsRepository.validateAddress(
                    contactUid,
                    ContactAddress(blockchain, parsedAddress.hex)
                )
                DataState.Success(parsedAddress)
            } catch (error: Throwable) {
                ensureActive()
                DataState.Error(error)
            }
            emitState()
        }
    }

    override fun createState() = UiState(
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

    private suspend fun parseAddress(addressParser: AddressParserChain, value: String): Address =
        withContext(Dispatchers.IO) {
            try {
                val resolvedAddress = addressParser.getAddressFromDomain(value)?.hex ?: value
                parse(resolvedAddress, addressParser.supportedAddressHandlers(resolvedAddress))
            } catch (error: Throwable) {
                throw AddressValidationException.Invalid(error, blockchain.name)
            }
        }

    private fun parse(value: String, supportedHandlers: List<IAddressHandler>): Address {
        if (supportedHandlers.isEmpty()) {
            throw AddressValidationException.Unsupported(blockchain.name)
        }

        try {
            return supportedHandlers.first().parseAddress(value)
        } catch (t: Throwable) {
            throw AddressValidationException.Invalid(t, blockchain.name)
        }
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
