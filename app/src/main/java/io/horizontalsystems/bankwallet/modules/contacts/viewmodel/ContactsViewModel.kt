package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository,
    mode: Mode
) : ViewModel() {

    private val readOnly = mode != Mode.Full
    private val showAddContact = !readOnly
    private val showMoreOptions = !readOnly

    private val contacts: List<Contact>
        get() = repository.contacts

    val exportJsonData: String
        get() = repository.export()

    val exportFileName: String
        get() = "UW_Contacts_${System.currentTimeMillis() / 1000}.json"

    var uiState by mutableStateOf(UiState(contacts, showAddContact, showMoreOptions))
        private set

    init {
        viewModelScope.launch {
            repository.contactsFlow.collect {
                emitState()
            }
        }
    }

    private fun emitState() {
        uiState = UiState(contacts, showAddContact, showMoreOptions)
    }

    fun importContacts(json: String) {
        repository.import(json)
    }

    data class UiState(
        val contacts: List<Contact>,
        val showAddContact: Boolean,
        val showMoreOptions: Boolean
    )

}
