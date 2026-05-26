package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.marketkit.models.BlockchainType

@HiltViewModel(assistedFactory = ChooseContactViewModel.Factory::class)
class ChooseContactViewModel @AssistedInject constructor(
    @Assisted private val blockchainType: BlockchainType,
    private val repository: ContactsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(blockchainType: BlockchainType): ChooseContactViewModel
    }

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

}

data class ContactViewItem(val name: String, val address: String)
