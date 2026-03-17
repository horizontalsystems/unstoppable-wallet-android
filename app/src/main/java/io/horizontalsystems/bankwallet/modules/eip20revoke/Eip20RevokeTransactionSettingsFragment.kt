package io.horizontalsystems.bankwallet.modules.eip20revoke

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object Eip20RevokeTransactionSettingsScreen : HSScreen(
    parentScreenClass = Eip20RevokeConfirmScreen::class
) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        Eip20RevokeTransactionSettingsScreen(backStack)
    }
}

class Eip20RevokeTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        Eip20RevokeTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20RevokeTransactionSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<Eip20RevokeConfirmViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(backStack)
}
