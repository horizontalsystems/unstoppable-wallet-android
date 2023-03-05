package cash.p.terminal.modules.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.modules.contacts.viewmodel.AddressViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactsViewModel

object ContactsModule {

    class ContactsViewModelFactory(
        private val repository: ContactsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactsViewModel(repository) as T

        }
    }

    class ContactViewModelFactory(
        private val repository: ContactsRepository,
        private val contactId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactViewModel(repository, contactId) as T
        }
    }

    class AddressViewModelFactory(
        private val contactAddress: ContactAddress?,
        private val definedAddresses: List<ContactAddress>?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressViewModel(App.evmBlockchainManager, App.marketKit, contactAddress, definedAddresses) as T
        }
    }

}
