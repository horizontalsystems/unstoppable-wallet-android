package cash.p.terminal.modules.blockchainstatus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.MoneroKitManager
import cash.p.terminal.core.managers.StellarKitManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.entities.BlockchainType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class BlockchainStatusFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<BlockchainType>(navController) { blockchainType ->
            val provider = rememberStatusProvider(blockchainType)
            val viewModel = koinViewModel<BlockchainStatusViewModel> {
                parametersOf(provider)
            }
            BlockchainStatusScreen(
                viewModel = viewModel,
                onBack = navController::navigateUp
            )
        }
    }
}

@Composable
private fun rememberStatusProvider(blockchainType: BlockchainType): BlockchainStatusProvider {
    return when (blockchainType) {
        BlockchainType.Tron -> {
            remember { TronBlockchainStatusProvider(App.tronKitManager) }
        }
        BlockchainType.Ton -> {
            remember { TonBlockchainStatusProvider(App.tonKitManager) }
        }
        BlockchainType.Monero -> {
            val moneroKitManager = koinInject<MoneroKitManager>()
            remember(moneroKitManager) { MoneroBlockchainStatusProvider(moneroKitManager) }
        }
        BlockchainType.Stellar -> {
            val stellarKitManager = koinInject<StellarKitManager>()
            remember(stellarKitManager) { StellarBlockchainStatusProvider(stellarKitManager) }
        }
        BlockchainType.Zcash -> {
            val walletManager = koinInject<IWalletManager>()
            val adapterManager = koinInject<IAdapterManager>()
            remember(walletManager, adapterManager) {
                ZcashBlockchainStatusProvider(walletManager, adapterManager)
            }
        }
        else -> error("Unsupported blockchain type for status: ${blockchainType.uid}")
    }
}
