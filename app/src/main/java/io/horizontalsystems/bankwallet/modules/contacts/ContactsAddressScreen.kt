package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.AddressScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import kotlinx.serialization.Serializable

@Serializable
data class ContactsAddressScreen(
    val contactUid: String,
    val address: ContactAddress?,
    val definedAddresses: List<ContactAddress>
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val resultBus = LocalResultEventBus.current
        val viewModel = viewModel<AddressViewModel>(
            factory = ContactsModule.AddressViewModelFactory(
                contactUid = contactUid,
                contactAddress = address,
                definedAddresses = definedAddresses
            )
        )

        AddressScreen(
            viewModel = viewModel,
            onNavigateToBlockchainSelector = {
                backStack.add(ContactsBlockchainSelectorScreen)
            },
            onDone = { contactAddress ->
                resultBus.sendResult(result = Result(added_address = contactAddress))

                backStack.removeLastOrNull()
            },
            onDelete = { contactAddress ->
                resultBus.sendResult(result = Result(deleted_address = contactAddress))

                backStack.removeLastOrNull()
            },
            onNavigateToBack = {
                backStack.removeLastOrNull()
            }
        )

    }

    data class Result(val added_address: ContactAddress? = null, val deleted_address: ContactAddress? = null)
}