package cash.p.terminal.modules.btcblockchainsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.core.App
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.blockchainstatus.BlockchainStatusScreen
import cash.p.terminal.modules.blockchainstatus.BlockchainStatusViewModel
import cash.p.terminal.modules.blockchainstatus.BtcBlockchainStatusProvider
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.entities.Blockchain
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SettingsPage = "settings"
private const val StatusPage = "status"

class BtcBlockchainSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Blockchain>(navController) { blockchain ->
            BtcBlockchainSettingsNavHost(blockchain, navController)
        }
    }
}

@Composable
private fun BtcBlockchainSettingsNavHost(
    blockchain: Blockchain,
    fragmentNavController: NavController
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SettingsPage,
    ) {
        composable(SettingsPage) {
            val viewModel = viewModel<BtcBlockchainSettingsViewModel>(
                factory = BtcBlockchainSettingsModule.Factory(blockchain)
            )
            BtcBlockchainSettingsScreen(
                uiState = viewModel.uiState,
                fragmentNavController = fragmentNavController,
                onSaveClick = viewModel::onSaveClick,
                onSelectRestoreMode = viewModel::onSelectRestoreMode,
                onCustomPeersChange = viewModel::onCustomPeersChange,
                onBlockchainStatusClick = { navController.navigate(StatusPage) }
            )
        }
        composablePage(StatusPage) {
            val provider = remember {
                BtcBlockchainStatusProvider(
                    blockchain = blockchain,
                    btcBlockchainManager = App.btcBlockchainManager,
                    walletManager = App.walletManager,
                    adapterManager = App.adapterManager
                )
            }
            val viewModel = koinViewModel<BlockchainStatusViewModel> {
                parametersOf(provider)
            }
            BlockchainStatusScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack
            )
        }
    }
}
