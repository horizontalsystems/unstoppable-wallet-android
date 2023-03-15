package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository
) : ViewModel() {

    private val contacts: List<Contact>
        get() = repository.contacts

    val exportJsonData: String
        get() = repository.export()

    val exportFileName: String
        get() = "UW_Contacts_${System.currentTimeMillis() / 1000}.json"

    var uiState by mutableStateOf(UiState(contacts))
        private set

    init {
        viewModelScope.launch {
            repository.contactsFlow.collect {
                emitState()
            }
        }
    }

    private fun emitState() {
        uiState = UiState(contacts)
    }

    fun importContacts(json: String) {
        repository.import(json)
    }

    data class UiState(
        val contacts: List<Contact>
    )

}
