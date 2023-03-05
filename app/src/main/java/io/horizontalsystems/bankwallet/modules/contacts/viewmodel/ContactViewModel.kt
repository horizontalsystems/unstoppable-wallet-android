package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import java.util.*

class ContactViewModel(
    private val repository: ContactsRepository,
    contactId: String? = null,
) : ViewModel() {

    private val contact = contactId?.let { repository.get(it) } ?: Contact(UUID.randomUUID().toString(), "", listOf())
    private val title = if (contactId == null)
        TranslatableString.ResString(R.string.Contacts_NewContact)
    else
        TranslatableString.PlainString(contact.name)

    private var contactName = contact.name
    private var addresses: MutableMap<Blockchain, ContactAddress> = contact.addresses.associateBy { it.blockchain }.toMutableMap()

    private var saveEnabled = false
    private var showDelete = contactId != null

    private var closeAfterSave = false

    var uiState by mutableStateOf(uiState())
        private set

    fun onNameChange(name: String) {
        contactName = name

        emitUiState()
    }

    fun onSave() {
        val editedContact = contact.copy(name = uiState.contactName, addresses = uiState.addresses)
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
        val savedAddresses = contact.addresses.toSet()
        val newAddresses = addresses.values.toSet()

        val addressesChanged = (savedAddresses.toMutableSet() + newAddresses) != savedAddresses

        Log.e("e", "addressesChanged = $addressesChanged")

        /*addresses.size != contact.addresses.size ||

            addresses.values.any { editedAddress ->
        contact.addresses.any { address -> address.blockchain == editedAddress.blockchain && address.address != editedAddress.address }
    }*/

        return contactName != contact.name || addressesChanged
    }

    private fun emitUiState() {
        uiState = uiState()
    }

    private fun uiState(): UiState {
        val addresses1 = this.addresses.values.sortedBy { it.blockchain.type.order }

        Log.e("e", "uiState() ${Integer.toHexString(System.identityHashCode(addresses1))} = " + addresses1.joinToString { it.address })

        return UiState(
            headerTitle = this.title,
            contactName = this.contactName,
            addresses = addresses1,
            saveEnabled = isSaveEnabled(),
            showDelete = this.showDelete,
            closeWithSuccess = this.closeAfterSave
        )
    }

    fun setAddress(address: ContactAddress?) {
        address?.let {
            addresses[address.blockchain] = address

            emitUiState()
        }
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
