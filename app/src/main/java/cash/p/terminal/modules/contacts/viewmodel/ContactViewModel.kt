package cash.p.terminal.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.order
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import java.util.*

class ContactViewModel(
    private val repository: ContactsRepository,
    existingContact: Contact?,
    newAddress: ContactAddress?
) : ViewModel() {

    private val contact = existingContact ?: Contact(UUID.randomUUID().toString(), "", listOf(), -1)
    private val title = if (existingContact == null)
        TranslatableString.ResString(R.string.Contacts_NewContact)
    else
        TranslatableString.PlainString(existingContact.name)

    private var contactName = contact.name
    private var addresses: MutableMap<Blockchain, ContactAddress> = contact.addresses.associateBy { it.blockchain }.toMutableMap()
    private var addressViewItems: List<AddressViewItem> = addressViewItems()
    private val newContact = existingContact == null
    private var closeAfterSave = false

    var uiState by mutableStateOf(uiState())
        private set

    init {
        newAddress?.let {
            setAddress(it)
        }
    }

    fun onNameChange(name: String) {
        contactName = name

        emitUiState()
    }

    fun onSave() {
        val editedContact = contact.copy(
            name = uiState.contactName,
            addresses = uiState.addressViewItems.map { it.contactAddress },
            modifiedTimestamp = System.currentTimeMillis() / 1000
        )
        repository.save(editedContact)

        closeAfterSave = true

        emitUiState()
    }

    fun onDelete() {
        repository.delete(contact.uid)

        closeAfterSave = true

        emitUiState()
    }

    private fun isSaveEnabled(): Boolean {
        val savedAddresses = contact.addresses.toSet()
        val newAddresses = addresses.values.toSet()
        val addressesChanged = (savedAddresses.toMutableSet() + newAddresses) != savedAddresses

        return contactName.isNotBlank() && (contactName != contact.name || addressesChanged)
    }

    private fun emitUiState() {
        uiState = uiState()
    }

    private fun addressViewItems(): List<AddressViewItem> {
        val sortedAddresses = addresses.values.sortedBy { it.blockchain.type.order }
        val savedAddresses = contact.addresses.associateBy { it.blockchain }
        return sortedAddresses.map { AddressViewItem(it, edited = it != savedAddresses[it.blockchain]) }
    }

    private fun uiState() = UiState(
        headerTitle = title,
        contactName = contactName,
        addressViewItems = addressViewItems,
        saveEnabled = isSaveEnabled(),
        showDelete = !newContact,
        closeWithSuccess = closeAfterSave
    )

    fun setAddress(address: ContactAddress) {
        addresses[address.blockchain] = address
        addressViewItems = addressViewItems()

        emitUiState()
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val contactName: String,
        val addressViewItems: List<AddressViewItem>,
        val saveEnabled: Boolean,
        val showDelete: Boolean,
        val closeWithSuccess: Boolean
    )

    data class AddressViewItem(
        val contactAddress: ContactAddress,
        val edited: Boolean
    ) {
        val blockchain: Blockchain
            get() = contactAddress.blockchain
    }

}
