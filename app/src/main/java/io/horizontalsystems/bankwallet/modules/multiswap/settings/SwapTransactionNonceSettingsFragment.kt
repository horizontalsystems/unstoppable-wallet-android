package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object SwapTransactionNonceSettingsScreen : HSScreen()

class SwapTransactionNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapTransactionNonceSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(navController: NavController) {
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}
