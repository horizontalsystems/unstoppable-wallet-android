package io.horizontalsystems.bankwallet.modules.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.BottomSheetSceneStrategy
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import kotlinx.serialization.Serializable

@Serializable
data object WalletSwitchScreen : HSScreen() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun getMetadata() = BottomSheetSceneStrategy.bottomSheet()

    @Composable
    override fun GetContent(
        backStack: MutableList<HSScreen>,
        resultBus: ResultEventBus
    ) {
        WalletSwitchScreen {
            backStack.removeLastOrNull()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSwitchScreen(onBack: () -> Unit) {
    val viewModel = viewModel<WalletSwitchViewModel>(factory = WalletSwitchViewModel.Factory())
    val uiState = viewModel.uiState

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = onBack,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WalletSwitchBottomSheet(
                wallets = uiState.wallets,
                watchingAddresses = uiState.watchWallets,
                selectedAccount = uiState.activeWallet,
                onSelectListener = { account ->
                    viewModel.onSelect(account)
                    onBack()

                    stat(
                        page = StatPage.SwitchWallet,
                        event = StatEvent.Select(StatEntity.Wallet)
                    )
                },
                onCancelClick = onBack
            )
        }
    }
}
