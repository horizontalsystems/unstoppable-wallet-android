package io.horizontalsystems.bankwallet.modules.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact

class SelectContactViewModel(
    contactsRepository: ContactsRepository,
) : ViewModelUiState<SelectContactUiState>() {
    private var items = listOf(null) + contactsRepository.contacts

    override fun createState() = SelectContactUiState(
        items = items
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectContactViewModel(App.contactsRepository) as T
        }
    }
}

data class SelectContactUiState(val items: List<Contact?>)
