package cash.p.terminal.modules.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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

}
