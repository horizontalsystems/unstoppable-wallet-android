package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.BlockchainType

class ChooseContactViewModel(
    private val repository: ContactsRepository,
    private val blockchainType: BlockchainType
) : ViewModel() {

    var items: List<ContactViewItem> by mutableStateOf(listOf())
        private set

    private var query: String? = null

    init {
        rebuildItems()
    }

    fun onEnterQuery(query: String?) {
        this.query = query

        rebuildItems()
    }

    private fun rebuildItems() {
        items = repository.getContactsFiltered(blockchainType, query)
            .map {
                ContactViewItem(
                    it.name,
                    it.addresses.first { it.blockchain.type == blockchainType }.address
                )
            }
    }

    class Factory(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChooseContactViewModel(App.contactsRepository, blockchainType) as T
        }
    }
}

data class ContactViewItem(val name: String, val address: String)
