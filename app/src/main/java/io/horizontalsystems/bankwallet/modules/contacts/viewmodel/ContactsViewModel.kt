package io.horizontalsystems.bankwallet.modules.contacts.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository,
    private val mode: Mode
) : ViewModel() {

    private val readOnly = mode != Mode.Full
    private val showAddContact = !readOnly
    private val showMoreOptions = !readOnly

    private var nameQuery: String? = null
    private val contacts: List<Contact>
        get() = repository.getContactsFiltered(nameQuery = nameQuery)

    val backupJson: String
        get() = repository.asJsonString

    val backupFileName: String
        get() = "UW_Contacts_${System.currentTimeMillis() / 1000}.json"

    var uiState by mutableStateOf(UiState(contacts, nameQuery, nameQuery != null, showAddContact, showMoreOptions))
        private set

    init {
        viewModelScope.launch {
            repository.contactsFlow.collect {
                emitState()
            }
        }
    }

    fun onEnterQuery(query: String?) {
        nameQuery = query
        emitState()
    }

    fun restore(json: String) {
        repository.restore(json)
    }

    fun shouldShowReplaceWarning(contact: Contact): Boolean {
        return mode is Mode.AddAddressToExistingContact && contact.addresses.any { it.blockchain.type == mode.blockchainType }
    }

    fun shouldShowRestoreWarning(): Boolean {
        return contacts.isNotEmpty()
    }

    fun replaceWarningMessage(contact: Contact): TranslatableString? {
        val blockchainType = (mode as? Mode.AddAddressToExistingContact)?.blockchainType ?: return null
        val address = (mode as? Mode.AddAddressToExistingContact)?.address ?: return null
        val oldAddress = contact.addresses.find { it.blockchain.type == blockchainType } ?: return null

        return TranslatableString.ResString(
            R.string.Contacts_AddAddress_ReplaceWarning,
            oldAddress.blockchain.name,
            oldAddress.address.shorten(),
            address.shorten()
        )
    }

    private fun emitState() {
        uiState = UiState(contacts, nameQuery, nameQuery != null, showAddContact, showMoreOptions)
    }

    data class UiState(
        val contacts: List<Contact>,
        val nameQuery: String?,
        val searchMode: Boolean,
        val showAddContact: Boolean,
        val showMoreOptions: Boolean
    )

}
