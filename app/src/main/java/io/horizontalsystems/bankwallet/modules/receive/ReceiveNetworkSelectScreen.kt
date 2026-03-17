package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.NetworkSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ReceiveNetworkSelectScreen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val activeAccount = viewModel.activeAccount
        val fullCoin = viewModel.fullCoin()
        if (activeAccount == null || fullCoin == null) {
            CloseWithMessage(backStack)
            return
        }
        NetworkSelectScreen(
            backStack = backStack,
            activeAccount = activeAccount,
            fullCoin = fullCoin,
            closeModule = { backStack.closeModule() },
            onSelect = { wallet ->
                onSelectWallet(wallet, backStack)
            }
        )
    }
}