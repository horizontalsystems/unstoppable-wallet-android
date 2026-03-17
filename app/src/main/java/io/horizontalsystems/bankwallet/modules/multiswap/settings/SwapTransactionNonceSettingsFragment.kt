package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object SwapTransactionNonceSettingsScreen : HSScreen(usePreviousScreenVmScope = true) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        SwapTransactionNonceSettingsScreen(backStack)
    }
}

class SwapTransactionNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<SwapConfirmViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(backStack)
}
