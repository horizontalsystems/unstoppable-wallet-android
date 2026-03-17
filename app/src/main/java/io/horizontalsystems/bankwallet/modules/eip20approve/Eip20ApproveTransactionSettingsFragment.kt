package io.horizontalsystems.bankwallet.modules.eip20approve

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object Eip20ApproveTransactionSettingsScreen : HSScreen(
    parentScreenClass = Eip20ApproveScreen::class
) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        Eip20ApproveTransactionSettingsScreen(backStack)
    }
}

class Eip20ApproveTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        Eip20ApproveTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20ApproveTransactionSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<Eip20ApproveViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(backStack)
}
