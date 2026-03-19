package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.NavController

class SwapTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapTransactionSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionSettingsScreen(navController: NavController) {
    val previousBackStackEntry = navController.previousBackStackEntry ?: run {
        navController.removeLastOrNull()
        return
    }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = previousBackStackEntry,
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}
