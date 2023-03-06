package cash.p.terminal.modules.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.marketkit.models.BlockchainType

class ChooseContactViewModel(
    private val repository: ContactsRepository,
    private val blockchainType: BlockchainType
) : ViewModel() {

    val items: List<ContactViewItem>

    init {
        items = repository.getContactsByBlockchainType(blockchainType)
            .map {
                ContactViewItem(it.name, it.addresses.first { it.blockchain.type == blockchainType }.address)
            }
    }

    class Factory(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChooseContactViewModel(ContactsRepository(), blockchainType) as T
        }
    }
}

data class ContactViewItem(val name: String, val address: String)
