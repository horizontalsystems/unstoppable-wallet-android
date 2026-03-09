package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import kotlinx.serialization.Serializable

@Serializable
data object SwapTransactionSettingsScreen : HSScreen(usePreviousScreenVmScope = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        SwapTransactionSettingsScreen(backStack)
    }
}

class SwapTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Composable
fun SwapTransactionSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<SwapConfirmViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(backStack)
}
