package cash.p.terminal.modules.contacts

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.modules.address.AddressHandlerFactory
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.modules.contacts.viewmodel.AddressViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.core.IPinComponent

object ContactsModule {

    class ContactsViewModelFactory(private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactsViewModel(
                repository = getKoinInstance<ContactsRepository>(),
                mode = mode,
                pinComponent = getKoinInstance<IPinComponent>()
            ) as T
        }
    }

    class ContactViewModelFactory(
        private val contact: Contact?,
        private val newAddress: ContactAddress?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactViewModel(getKoinInstance<ContactsRepository>(), contact, newAddress) as T
        }
    }

    class AddressViewModelFactory(
        private val contactUid: String?,
        private val contactAddress: ContactAddress?,
        private val definedAddresses: List<ContactAddress>?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressViewModel(
                contactUid,
                getKoinInstance<ContactsRepository>(),
                AddressHandlerFactory(AppConfigProvider.udnApiKey),
                App.marketKit,
                contactAddress,
                definedAddresses
            ) as T
        }
    }

    enum class AddAddressAction(@StringRes val title: Int) {
        AddToNewContact(R.string.Contacts_AddAddress_NewContact),
        AddToExistingContact(R.string.Contacts_AddAddress_ExistingContact)
    }

    sealed class ContactValidationException(override val message: String?) : Throwable() {
        object DuplicateContactName :
            ContactValidationException(cash.p.terminal.strings.helpers.Translator.getString(R.string.Contacts_Error_DefinedName))

        class DuplicateAddress(val contact: Contact) :
            ContactValidationException(
                cash.p.terminal.strings.helpers.Translator.getString(
                    R.string.Contacts_Error_DefinedAddress,
                    contact.name
                )
            )
    }


}
