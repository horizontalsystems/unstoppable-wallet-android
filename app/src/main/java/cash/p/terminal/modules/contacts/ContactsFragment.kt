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
    val repository = ContactsRepository()

    AnimatedNavHost(
        navController = navHostController,
        startDestination = "contacts",
    ) {
        composable("contacts") {
            val viewModel = viewModel<ContactsViewModel>(factory = ContactsModule.ContactsViewModelFactory(repository))
            ContactsScreen(
                viewModel = viewModel,
                onNavigateToBack = { navController.popBackStack() },
                onNavigateToCreateContact = { navHostController.navigate("contact") },
                onNavigateToContact = { contactId -> navHostController.navigate("contact?id=$contactId") }
            )
        }
        composablePage(
            route = "contact?id={contactId}",
            arguments = listOf(navArgument("contactId") { nullable = true })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")
            val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(repository, contactId))

            ContactScreen(
                viewModel = viewModel,
                onNavigateToBack = {
                    navHostController.popBackStack()
                },
                onNavigateToAddress = { address ->

                    navHostController.getNavigationResult("contacts_address_result") { bundle ->
                        val editedAddress = bundle.getParcelable<ContactAddress>("contact_address")
                        viewModel.setAddress(editedAddress)
                    }

                    backStackEntry.savedStateHandle["address"] = address
                    backStackEntry.savedStateHandle["defined_addresses"] = viewModel.uiState.addresses

                    backStackEntry.savedStateHandle["test"] = Contact("", "", listOf())

                    navHostController.navigate("address")
                }
            )
        }
        composablePage(
            route = "address"
        ) {
            //TODO this place is called 6 times
            val address = navHostController.previousBackStackEntry?.savedStateHandle?.get<ContactAddress>("address")
            val definedAddresses = navHostController.previousBackStackEntry?.savedStateHandle?.get<List<ContactAddress>>("defined_addresses")

            val viewModel = viewModel<AddressViewModel>(
                factory = ContactsModule.AddressViewModelFactory(
                    contactAddress = address,
                    definedAddresses = definedAddresses
                )
            )

            AddressNavHost(
                viewModel = viewModel,
                onDone = { contactAddress ->
                    navHostController.setNavigationResult(
                        "contacts_address_result",
                        bundleOf("contact_address" to contactAddress)
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
    onDone: (ContactAddress) -> Unit,
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
                    onDone(contactAddress)

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
