package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class OpenCryptoPayEvmSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
            navController.getBackStackEntry(R.id.openCryptoPayEvmConfirmationFragment)
        }
        val viewModel = viewModel<OpenCryptoPayEvmConfirmationViewModel>(
            viewModelStoreOwner = viewModelStoreOwner,
        )
        viewModel.sendTransactionService.GetSettingsContent(navController)
    }
}

class OpenCryptoPayEvmNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
            navController.getBackStackEntry(R.id.openCryptoPayEvmConfirmationFragment)
        }
        val viewModel = viewModel<OpenCryptoPayEvmConfirmationViewModel>(
            viewModelStoreOwner = viewModelStoreOwner,
        )
        viewModel.sendTransactionService.GetNonceSettingsContent(navController)
    }
}
