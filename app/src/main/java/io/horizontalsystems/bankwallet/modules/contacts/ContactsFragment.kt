package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.screen.AddressScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.BlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactsScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.core.setNavigationResult

class ContactsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ContactsNavHost(
                    navController = findNavController(),
                    mode = arguments?.parcelable(modeKey) ?: Mode.Full
                )
            }
        }
    }

    companion object {
        private const val modeKey = "modeKey"

        fun prepareParams(mode: Mode): Bundle {
            return bundleOf(modeKey to mode)
        }
    }
}

@Composable
fun ContactsNavHost(navController: NavController, mode: Mode) {
    val navHostController = rememberNavController()

    val startDestination: String
    val addAddress: ContactAddress?

    when (mode) {
        is Mode.AddAddressToExistingContact -> {
            startDestination = "contacts"
            addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                ContactAddress(blockchain, mode.address)
            }
        }
        is Mode.AddAddressToNewContact -> {
            startDestination = "contact"
            addAddress = App.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                ContactAddress(blockchain, mode.address)
            }
        }
        Mode.Full -> {
            startDestination = "contacts"
            addAddress = null
        }
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination
    ) {
        composable("contacts") { backStackEntry ->
            val viewModel = viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory(mode))
            ContactsScreen(
                viewModel = viewModel,
                onNavigateToBack = { navController.popBackStack() },
                onNavigateToCreateContact = { navHostController.navigate("contact") },
                onNavigateToContact = { contact ->
                    backStackEntry.savedStateHandle["contact"] = contact
                    backStackEntry.savedStateHandle["new_address"] = addAddress

                    navHostController.navigate("contact")
                }
            )
        }
        composablePage(route = "contact") { backStackEntry ->
            val contact = navHostController.previousBackStackEntry?.savedStateHandle?.get<Contact>("contact")
            val newAddress = navHostController.previousBackStackEntry?.savedStateHandle?.get<ContactAddress>("new_address") ?: addAddress

            navHostController.previousBackStackEntry?.savedStateHandle?.set("contact", null)
            navHostController.previousBackStackEntry?.savedStateHandle?.set("new_address", null)

            val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(contact, newAddress))

            ContactScreen(
                viewModel = viewModel,
                onNavigateToBack = {
                    if (mode == Mode.Full) {
                        navHostController.popBackStack()
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigateToAddress = { address ->
                    navHostController.getNavigationResult("contacts_address_result") { bundle ->
                        bundle.parcelable<ContactAddress>("added_address")?.let { editedAddress ->
                            viewModel.setAddress(editedAddress)
                        }
                        bundle.parcelable<ContactAddress>("deleted_address")?.let { deletedAddress ->
                            viewModel.deleteAddress(deletedAddress)
                        }
                    }

                    backStackEntry.savedStateHandle["contact_uid"] = viewModel.contact.uid
                    backStackEntry.savedStateHandle["address"] = address
                    backStackEntry.savedStateHandle["defined_addresses"] = viewModel.uiState.addressViewItems.map { it.contactAddress }

                    navHostController.navigate("address")
                }
            )
        }
        composablePage(
            route = "address"
        ) {
            val contactUid = navHostController.previousBackStackEntry?.savedStateHandle?.get<String>("contact_uid")
            val address = navHostController.previousBackStackEntry?.savedStateHandle?.get<ContactAddress>("address")
            val definedAddresses = navHostController.previousBackStackEntry?.savedStateHandle?.get<List<ContactAddress>>("defined_addresses")

            val viewModel = viewModel<AddressViewModel>(
                factory = ContactsModule.AddressViewModelFactory(
                    contactUid = contactUid,
                    contactAddress = address,
                    definedAddresses = definedAddresses
                )
            )

            AddressNavHost(
                viewModel = viewModel,
                onAddAddress = { contactAddress ->
                    navHostController.setNavigationResult(
                        "contacts_address_result",
                        bundleOf("added_address" to contactAddress)
                    )
                },
                onDeleteAddress = { contactAddress ->
                    navHostController.setNavigationResult(
                        "contacts_address_result",
                        bundleOf("deleted_address" to contactAddress)
                    )
                },
                onCloseNavHost = { navHostController.popBackStack() }
            )
        }
    }
}

@Composable
fun AddressNavHost(
    viewModel: AddressViewModel,
    onAddAddress: (ContactAddress) -> Unit,
    onDeleteAddress: (ContactAddress) -> Unit,
    onCloseNavHost: () -> Unit
) {
    val navHostController = rememberNavController()

    NavHost(
        navController = navHostController,
        startDestination = "address",
    ) {
        composablePage(route = "address") {
            AddressScreen(
                viewModel = viewModel,
                onNavigateToBlockchainSelector = {
                    navHostController.navigate("blockchainSelector")
                },
                onDone = { contactAddress ->
                    onAddAddress(contactAddress)

                    onCloseNavHost()
                },
                onDelete = { contactAddress ->
                    onDeleteAddress(contactAddress)

                    onCloseNavHost()
                },
                onNavigateToBack = {
                    onCloseNavHost()
                }
            )
        }
        composablePage(route = "blockchainSelector") {
            BlockchainSelectorScreen(
                blockchains = viewModel.uiState.availableBlockchains,
                selectedBlockchain = viewModel.uiState.blockchain,
                onSelectBlockchain = { blockchain ->
                    viewModel.onEnterBlockchain(blockchain)

                    navHostController.popBackStack()
                },
                onNavigateToBack = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}
