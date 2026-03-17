package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ReceiveDerivationSelectScreen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(backStack)
            return
        }
        val derivationViewModel = viewModel<DerivationSelectViewModel>(
            factory = DerivationSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = derivationViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
            onSelect = { wallet ->
                onSelectWallet(wallet, backStack)
            },
            closeModule = { backStack.closeModule() },
            onBackPress = { backStack.removeLastOrNull() }
        )
    }
}