package cash.p.terminal.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository
) : ViewModel() {

    val contacts: List<Contact>
        get() = repository.contacts

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

    data class UiState(
        val contacts: List<Contact>
    )

}
