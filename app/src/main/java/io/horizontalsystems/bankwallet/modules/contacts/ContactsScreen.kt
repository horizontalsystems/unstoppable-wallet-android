package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactsScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import kotlinx.serialization.Serializable

@Serializable
data class ContactsScreen(val mode: Mode, val addAddress: ContactAddress?) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel =
            viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory(mode))
        ContactsScreen(
            viewModel = viewModel,
            onNavigateToBack = { backStack.removeLastUntil(ContactsRouterScreen::class, true) },
            onNavigateToCreateContact = { backStack.add(ContactScreen(mode)) },
            onNavigateToContact = { contact ->
                backStack.add(ContactScreen(mode, contact, addAddress))
            }
        )
    }
}