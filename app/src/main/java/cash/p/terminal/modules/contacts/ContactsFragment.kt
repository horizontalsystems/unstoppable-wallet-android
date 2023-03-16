package cash.p.terminal.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.contacts.screen.AddAddressScreen
import cash.p.terminal.modules.contacts.screen.ContactScreen
import cash.p.terminal.modules.contacts.screen.ContactsScreen
import cash.p.terminal.modules.contacts.screen.NewContactScreen

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

object Route {
    const val Contacts = "contacts"
    const val Contact = "contact"
    const val NewContact = "new_contact"
    const val AddAddress = "add_address"
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContactsNavHost(navController: NavController) {
    val navHostController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navHostController,
        startDestination = Route.Contacts,
    ) {
        composable(Route.Contacts) {
            ContactsScreen(
                navController,
                navHostController
            )
        }
        composablePage(Route.Contact) {
            ContactScreen(
                navHostController
            )
        }
        composablePage(Route.NewContact) {
            NewContactScreen(
                navHostController
            )
        }
        composablePage(Route.AddAddress) {
            AddAddressScreen(
                navHostController
            )
        }
    }
}
