package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.contacts.screen.AddAddressScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactScreen
import io.horizontalsystems.bankwallet.modules.contacts.screen.ContactsScreen

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
                viewModel,
                onNavigateToBack = { navController.popBackStack() },
                onNavigateToCreateContact = { navHostController.navigate("contact") },
                onNavigateToContact = { contactId -> navHostController.navigate("contact?contactId=$contactId") }
            )
        }
        composablePage(
            route = "contact?contactId={contactId}",
            arguments = listOf(navArgument("contactId") { nullable = true })
        ) { backstackEntry ->
            val contactId = backstackEntry.arguments?.getString("contactId")
            val viewModel = viewModel<ContactViewModel>(factory = ContactsModule.ContactViewModelFactory(repository, contactId))

            ContactScreen(
                viewModel,
                onNavigateToBack = {
                    navHostController.popBackStack()
                },
                onNavigateToAddAddress = {
                    navHostController.navigate("addAddress")
                }
            )
        }
        composablePage("addAddress") {
            AddAddressScreen(
                navHostController
            )
        }
    }
}
