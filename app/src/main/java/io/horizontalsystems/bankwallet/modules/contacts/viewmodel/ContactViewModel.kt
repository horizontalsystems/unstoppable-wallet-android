package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule.ContactValidationException
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import java.util.UUID

class ContactViewModel(
    private val repository: ContactsRepository,
    existingContact: Contact?,
    newAddress: ContactAddress?
) : ViewModelUiState<ContactViewModel.UiState>() {

    val contact = existingContact ?: Contact(UUID.randomUUID().toString(), "", listOf())
    private val title = if (existingContact == null)
        TranslatableString.ResString(R.string.Contacts_NewContact)
    else
        TranslatableString.PlainString(existingContact.name)

    private var contactName = contact.name
    private var addresses: MutableMap<Blockchain, ContactAddress> = contact.addresses.associateBy { it.blockchain }.toMutableMap()
    private var addressViewItems: List<AddressViewItem> = addressViewItems()
    private val isNewContact = existingContact == null
    private var closeAfterSave = false
    private var error: ContactValidationException? = null
    
    init {
        newAddress?.let {
            setAddress(it)
        }
    }

    override fun createState() = UiState(
        headerTitle = title,
        addressViewItems = addressViewItems,
        saveEnabled = isSaveEnabled(),
        confirmBack = hasChanges(),
        showDelete = !isNewContact,
        focusOnContactName = isNewContact,
        closeWithSuccess = closeAfterSave,
        error = error
    )

    fun onNameChange(name: String) {
        contactName = name

        error = try {
            repository.validateContactName(contactUid = contact.uid, name = name)
            null
        } catch (ex: ContactValidationException.DuplicateContactName) {
            ex
        }

        emitState()
    }

    fun onSave() {
        val editedContact = contact.copy(
            name = contactName,
            addresses = uiState.addressViewItems.map { it.contactAddress }
        )
        repository.save(editedContact)

        closeAfterSave = true

        emitState()
    }

    fun onDelete() {
        repository.delete(contact.uid)

        closeAfterSave = true

        emitState()
    }

    fun setAddress(address: ContactAddress) {
        addresses[address.blockchain] = address
        addressViewItems = addressViewItems()

        emitState()
    }

    fun deleteAddress(address: ContactAddress) {
        addresses.remove(address.blockchain)
        addressViewItems = addressViewItems()

        emitState()
    }

    private fun hasChanges(): Boolean {
        val savedAddresses = contact.addresses.toSet()
        val newAddresses = addresses.values.toSet()
        val addressesChanged = savedAddresses.size != newAddresses.size || (savedAddresses.toMutableSet() + newAddresses) != savedAddresses

        return contactName != contact.name || addressesChanged
    }

    private fun isSaveEnabled(): Boolean {
        return addresses.isNotEmpty() && error == null && contactName.isNotBlank() && hasChanges()
    }

    private fun addressViewItems(): List<AddressViewItem> {
        val sortedAddresses = addresses.values.sortedBy { it.blockchain.type.order }
        val savedAddresses = contact.addresses.associateBy { it.blockchain }
        return sortedAddresses.map { AddressViewItem(it, edited = it != savedAddresses[it.blockchain]) }.sortedByDescending {
            it.edited
        }
    }

    data class UiState(
        val headerTitle: TranslatableString,
        val addressViewItems: List<AddressViewItem>,
        val saveEnabled: Boolean,
        val confirmBack: Boolean,
        val showDelete: Boolean,
        val focusOnContactName: Boolean,
        val closeWithSuccess: Boolean,
        val error: ContactValidationException?
    )

    data class AddressViewItem(
        val contactAddress: ContactAddress,
        val edited: Boolean
    ) {
        val blockchain: Blockchain
            get() = contactAddress.blockchain
    }

}
