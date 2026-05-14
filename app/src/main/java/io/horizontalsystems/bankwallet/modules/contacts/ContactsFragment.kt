package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.AddressScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.BlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactsScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
data class ContactsFragment(val input: Input) : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mode = input.mode

        val startDestination: HSScreen
        val addAddress: ContactAddress?

        when (mode) {
            is Mode.AddAddressToExistingContact -> {
                addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }
                startDestination = ContactsPage(mode, addAddress)
            }
            is Mode.AddAddressToNewContact -> {
                addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }
                startDestination = ContactPage(null, addAddress)
            }
            Mode.Full -> {
                addAddress = null
                startDestination = ContactsPage(mode, addAddress)
            }
        }

        startDestination.GetContent(navController)
    }

    @Serializable
    data class Input(val mode: Mode)
}

@Serializable
data class ContactsPage(val mode: Mode, val addAddress: ContactAddress?) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory(mode))
        ContactsScreen(
            viewModel = viewModel,
            onNavigateToBack = { navController.removeLastOrNull() },
            onNavigateToCreateContact = { navController.add(ContactPage(null, null)) },
            onNavigateToContact = { contact ->
                navController.add(ContactPage(contact, addAddress))
            }
        )
    }
}

@Serializable
data class ContactPage(
    val contact: Contact?,
    val newAddress: ContactAddress?
) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(contact, newAddress))

        // todo: find better solution
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        ResultEffect<AddressPage.Result>(resultKeyUuid = uuid) {
            it.added_address?.let { editedAddress ->
                viewModel.setAddress(editedAddress)
            }
            it.deleted_address?.let { deletedAddress ->
                viewModel.deleteAddress(deletedAddress)
            }
        }

        ContactScreen(
            viewModel = viewModel,
            onNavigateToBack = {
                navController.removeLastOrNull()
            },
            onNavigateToAddress = { address ->
                val screen = AddressPage(
                    contactUid = viewModel.contact.uid,
                    address = address,
                    definedAddresses = viewModel.uiState.addressViewItems.map { it.contactAddress },
                )

                screen.resultKey = uuid
                navController.add(screen)
            }
        )
    }
}

@Serializable
data class AddressPage(
    val contactUid: String,
    val address: ContactAddress?,
    val definedAddresses: List<ContactAddress>
) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<AddressViewModel>(
            factory = ContactsModule.AddressViewModelFactory(
                contactUid = contactUid,
                contactAddress = address,
                definedAddresses = definedAddresses
            )
        )

        val resultEventBus = LocalResultEventBus.current

        AddressScreen(
            viewModel = viewModel,
            onNavigateToBlockchainSelector = {
                navController.add(BlockchainSelectorPage)
            },
            onDone = { contactAddress ->
                resultEventBus.sendResult<Result>(Result(added_address = contactAddress))
                navController.removeLastOrNull()
            },
            onDelete = { contactAddress ->
                resultEventBus.sendResult<Result>(Result(deleted_address = contactAddress))
                navController.removeLastOrNull()
            },
            onNavigateToBack = {
                navController.removeLastOrNull()
            }
        )
    }

    data class Result(val added_address: ContactAddress? = null, val deleted_address: ContactAddress? = null)
}


@Serializable
data object BlockchainSelectorPage : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<AddressViewModel>(AddressPage::class)

        BlockchainSelectorScreen(
            blockchains = viewModel.uiState.availableBlockchains,
            selectedBlockchain = viewModel.uiState.blockchain,
            onSelectBlockchain = { blockchain ->
                viewModel.onEnterBlockchain(blockchain)

                navController.removeLastOrNull()
            },
            onNavigateToBack = {
                navController.removeLastOrNull()
            }
        )
    }
}
