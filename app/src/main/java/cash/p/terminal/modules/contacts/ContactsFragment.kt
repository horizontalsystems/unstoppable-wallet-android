package cash.p.terminal.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.modules.contacts.screen.AddressScreen
import cash.p.terminal.modules.contacts.screen.BlockchainSelectorScreen
import cash.p.terminal.modules.contacts.screen.ContactScreen
import cash.p.terminal.modules.contacts.screen.ContactsScreen
import cash.p.terminal.modules.contacts.viewmodel.AddressViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactViewModel
import cash.p.terminal.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.core.getNavigationResult
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
                ContactsNavHost(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContactsNavHost(navController: NavController) {
    val navHostController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navHostController,
        startDestination = "contacts",
    ) {
        composable("contacts") { backStackEntry ->
            val viewModel = viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory())
            ContactsScreen(
                viewModel = viewModel,
                onNavigateToBack = { navController.popBackStack() },
                onNavigateToCreateContact = { navHostController.navigate("contact") },
                onNavigateToContact = { contact ->
                    backStackEntry.savedStateHandle["contact"] = contact
                    backStackEntry.savedStateHandle["new_address"] = null

                    navHostController.navigate("contact")
                }
            )
        }
        composablePage(route = "contact") { backStackEntry ->
            val contact = navHostController.previousBackStackEntry?.savedStateHandle?.get<Contact>("contact")
            val newAddress = navHostController.previousBackStackEntry?.savedStateHandle?.get<ContactAddress>("new_address")

            navHostController.previousBackStackEntry?.savedStateHandle?.set("contact", null)
            navHostController.previousBackStackEntry?.savedStateHandle?.set("new_address", null)

            val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(contact, newAddress))

            ContactScreen(
                viewModel = viewModel,
                onNavigateToBack = {
                    navHostController.popBackStack()
                },
                onNavigateToAddress = { address ->

                    navHostController.getNavigationResult("contacts_address_result") { bundle ->
                        bundle.getParcelable<ContactAddress>("added_address")?.let { editedAddress ->
                            viewModel.setAddress(editedAddress)
                        }
                        bundle.getParcelable<ContactAddress>("deleted_address")?.let { deletedAddress ->
                            viewModel.deleteAddress(deletedAddress)
                        }
                    }

                    backStackEntry.savedStateHandle["address"] = address
                    backStackEntry.savedStateHandle["defined_addresses"] = viewModel.uiState.addressViewItems.map { it.contactAddress }

                    navHostController.navigate("address")
                }
            )
        }
        composablePage(
            route = "address"
        ) {
            val address = navHostController.previousBackStackEntry?.savedStateHandle?.get<ContactAddress>("address")
            val definedAddresses = navHostController.previousBackStackEntry?.savedStateHandle?.get<List<ContactAddress>>("defined_addresses")

            val viewModel = viewModel<AddressViewModel>(
                factory = ContactsModule.AddressViewModelFactory(contactAddress = address, definedAddresses = definedAddresses)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddressNavHost(
    viewModel: AddressViewModel,
    onAddAddress: (ContactAddress) -> Unit,
    onDeleteAddress: (ContactAddress) -> Unit,
    onCloseNavHost: () -> Unit
) {
    val navHostController = rememberAnimatedNavController()

    AnimatedNavHost(
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
