package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForPrevScreen

class SwapTransactionNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        SwapTransactionNonceSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = navController.viewModelForPrevScreen<SwapConfirmViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}
