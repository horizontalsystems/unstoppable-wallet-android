package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.AddressScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.BlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactsScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ContactsScreen(val mode: Mode = Mode.Full) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val screen = when (mode) {
            is Mode.AddAddressToExistingContact -> {
                val addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }

                contacts(mode, addAddress)
            }
            is Mode.AddAddressToNewContact -> {
                val addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }
                contact(mode, addAddress = addAddress)
            }
            Mode.Full -> {
                contacts(mode, null as Nothing?)
            }
        }

        screen.GetContent(backStack, resultBus)
    }
}

class ContactsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(val mode: Mode) : Parcelable
}

@Serializable
data class contacts(val mode: Mode, val addAddress: ContactAddress?) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory(mode))
        ContactsScreen(
            viewModel = viewModel,
            onNavigateToBack = { backStack.removeLastUntil(ContactsScreen::class, true) },
            onNavigateToCreateContact = { backStack.add(contact(mode)) },
            onNavigateToContact = { contact ->
                backStack.add(contact(mode, contact, addAddress))
            }
        )
    }
}

@Serializable
data class contact(
    val mode: Mode,
    val contact1: Contact? = null,
    val addAddress: ContactAddress? = null
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val contact = contact1
        val newAddress = addAddress

        val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(contact, newAddress))

        ResultEffect<address.Result>(resultBus) { result ->
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
                    backStack.removeLastUntil(ContactsScreen::class, true)
                }
            },
            onNavigateToAddress = { address ->
                backStack.add(
                    address(
                        viewModel.contact.uid,
                        address,
                        viewModel.uiState.addressViewItems.map { it.contactAddress }
                    )
                )
            }
        )
    }
}

@Serializable
data class address(
    val contactUid: String,
    val address: ContactAddress?,
    val definedAddresses: List<ContactAddress>
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
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
                backStack.add(blockchainSelector)
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

@Serializable
data object blockchainSelector : HSScreen(
    parentScreenClass = address::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<AddressViewModel>()
        BlockchainSelectorScreen(
            blockchains = viewModel.uiState.availableBlockchains,
            selectedBlockchain = viewModel.uiState.blockchain,
            onSelectBlockchain = { blockchain ->
                viewModel.onEnterBlockchain(blockchain)

                backStack.removeLastOrNull()
            },
            onNavigateToBack = {
                backStack.removeLastOrNull()
            }
        )
    }
}
