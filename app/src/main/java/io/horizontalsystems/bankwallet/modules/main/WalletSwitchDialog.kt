package io.horizontalsystems.bankwallet.modules.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent

class WalletSwitchDialog : BaseComposableBottomSheetFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        WalletSwitchScreen(navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSwitchScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = viewModel<WalletSwitchViewModel>(factory = WalletSwitchViewModel.Factory())
    val uiState = viewModel.uiState

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {
                navController.removeLastOrNull()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WalletSwitchBottomSheet(
                wallets = uiState.wallets,
                watchingAddresses = uiState.watchWallets,
                selectedAccount = uiState.activeWallet,
                onSelectListener = { account ->
                    viewModel.onSelect(account)
                    navController.removeLastOrNull()

                    stat(
                        page = StatPage.SwitchWallet,
                        event = StatEvent.Select(StatEntity.Wallet)
                    )
                },
                onCancelClick = {
                    navController.removeLastOrNull()
                }
            )
        }
    }
}
