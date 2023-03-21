package io.horizontalsystems.bankwallet.modules.contacts

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel

object ContactsModule {

    class ContactsViewModelFactory(private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactsViewModel(App.contactsRepository, mode) as T
        }
    }

    class ContactViewModelFactory(
        private val contact: Contact?,
        private val newAddress: ContactAddress?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactViewModel(App.contactsRepository, contact, newAddress) as T
        }
    }

    class AddressViewModelFactory(
        private val contactUid: String?,
        private val contactAddress: ContactAddress?,
        private val definedAddresses: List<ContactAddress>?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressViewModel(contactUid, App.contactsRepository, App.evmBlockchainManager, App.marketKit, contactAddress, definedAddresses) as T
        }
    }

    enum class ContactsAction(@StringRes val title: Int) {
        Restore(R.string.Contacts_Restore),
        Backup(R.string.Contacts_Backup)
    }

    enum class AddAddressAction(@StringRes val title: Int) {
        AddToNewContact(R.string.Contacts_AddAddress_NewContact),
        AddToExistingContact(R.string.Contacts_AddAddress_ExistingContact)
    }

    sealed class ContactValidationException(override val message: String?) : Throwable() {
        object DuplicateContactName : ContactValidationException(Translator.getString(R.string.Contacts_Error_DefinedName))
        class DuplicateAddress(val contact: Contact) :
            ContactValidationException(Translator.getString(R.string.Contacts_Error_DefinedAddress, contact.name))
    }


}
