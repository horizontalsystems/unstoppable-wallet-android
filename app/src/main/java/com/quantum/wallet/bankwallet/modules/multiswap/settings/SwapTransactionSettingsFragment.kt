package com.quantum.wallet.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.multiswap.SwapConfirmViewModel

class SwapTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapTransactionSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionSettingsScreen(navController: NavController) {
    val previousBackStackEntry = navController.previousBackStackEntry ?: run {
        navController.popBackStack()
        return
    }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = previousBackStackEntry,
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}
