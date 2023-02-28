package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import java.util.*

class ContactViewModel(
    private val repository: ContactsRepository,
    contactId: String? = null,
) : ViewModel() {

    private val contact = contactId?.let { repository.get(it) } ?: Contact(UUID.randomUUID().toString(), "", listOf())
    private val title = if (contactId == null) TranslatableString.ResString(R.string.Contacts_NewContact) else TranslatableString.PlainString(contact.name)

    private var contactName = contact.name
    private var addresses: List<ContactAddress> = contact.addresses

    private var saveEnabled = false
    private var showDelete = contactId != null

    private var closeAfterSave = false

    var uiState by mutableStateOf(UiState(this.title, this.contactName, this.addresses, this.saveEnabled, this.showDelete, this.closeAfterSave))
        private set

    fun onNameChange(name: String) {
        contactName = name

        emitUiState()
    }

    fun onSave() {
        val editedContact = contact.copy(name = contactName, addresses = addresses)
        repository.save(editedContact)

        closeAfterSave = true

        emitUiState()
    }

    fun onDelete() {
        repository.delete(contact.id)

        closeAfterSave = true

        emitUiState()
    }

    private fun isSaveEnabled(): Boolean {
        return contactName != contact.name // TODO add addresses check
    }

    private fun emitUiState() {
        uiState = UiState(this.title, this.contactName, this.addresses, isSaveEnabled(), this.showDelete, this.closeAfterSave)
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val contactName: String,
        val addresses: List<ContactAddress>,
        val saveEnabled: Boolean,
        val showDelete: Boolean,
        val closeWithSuccess: Boolean
    )

}
