package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ReceiveZcashAddressTypeSelectScreen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val wallet = viewModel.wallet
        if (wallet == null) {
            CloseWithMessage(backStack)
            return
        }

        ZcashAddressTypeSelectScreen(
            onZcashAddressTypeClick = { isTransparent ->
                onSelectWallet(wallet, backStack, isTransparent)
            },
            onBackPress = { backStack.removeLastOrNull() },
            closeModule = { backStack.closeModule() }
        )
    }
}