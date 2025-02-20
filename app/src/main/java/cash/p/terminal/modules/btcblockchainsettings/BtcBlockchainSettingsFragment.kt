package cash.p.terminal.modules.btcblockchainsettings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.requireInput

class BtcBlockchainSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = viewModel<BtcBlockchainSettingsViewModel>(
            factory = BtcBlockchainSettingsModule.Factory(navController.requireInput())
        )
        BtcBlockchainSettingsScreen(
            uiState = viewModel.uiState,
            navController = navController,
            onSaveClick = viewModel::onSaveClick,
            onSelectRestoreMode = viewModel::onSelectRestoreMode,
            onCustomPeersChange = viewModel::onCustomPeersChange
        )
    }

}
