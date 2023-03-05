package cash.p.terminal.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.order
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType

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

    private val canChangeBlockchain = contactAddress == null
    private var doneEnabled = true //address.isNotEmpty()
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

    var uiState by mutableStateOf(uiState())
        private set

    fun onEnterAddress(address: String) {
        this.address = address

        emitUiState()
    }

    fun onEnterBlockchain(blockchain: Blockchain) {
        this.blockchain = blockchain

        emitUiState()
    }

    fun onDone() {
//        val editedContact = contact.copy(name = contactName, addresses = addresses)
//        repository.save(editedContact)
//
//        closeAfterSave = true
//
//        emitUiState()
    }


//    private fun isSaveEnabled(): Boolean {
//        return contactName != contact.name // TODO add addresses check
//    }

    private fun uiState() = UiState(
        headerTitle = title,
        address = address,
        blockchain = blockchain,
        canChangeBlockchain = canChangeBlockchain,
        availableBlockchains = availableBlockchains,
        doneEnabled = doneEnabled
    )

    private fun emitUiState() {
        uiState = uiState()
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val address: String,
        val blockchain: Blockchain,
        val canChangeBlockchain: Boolean,
        val availableBlockchains: List<Blockchain>,
        val doneEnabled: Boolean
    )
}
