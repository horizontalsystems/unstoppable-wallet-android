package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import kotlinx.serialization.Serializable

@Serializable
data class ContactScreen(
    val mode: Mode,
    val contact1: Contact? = null,
    val addAddress: ContactAddress? = null
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val contact = contact1
        val newAddress = addAddress

        val viewModel = viewModel<ContactViewModel>(
            factory = ContactsModule.ContactViewModelFactory(
                contact,
                newAddress
            )
        )

        ResultEffect<ContactsAddressScreen.Result> { result ->
            result.added_address?.let {
                viewModel.setAddress(it)
            }
            result.deleted_address?.let {
                viewModel.deleteAddress(it)
            }
        }

        ContactScreen(
            viewModel = viewModel,
            onNavigateToBack = {
                if (mode == Mode.Full) {
                    backStack.removeLastOrNull()
                } else {
                    backStack.removeLastUntil(ContactsRouterScreen::class, true)
                }
            },
            onNavigateToAddress = { address ->
                backStack.add(
                    ContactsAddressScreen(
                        viewModel.contact.uid,
                        address,
                        viewModel.uiState.addressViewItems.map { it.contactAddress }
                    )
                )
            }
        )
    }
}