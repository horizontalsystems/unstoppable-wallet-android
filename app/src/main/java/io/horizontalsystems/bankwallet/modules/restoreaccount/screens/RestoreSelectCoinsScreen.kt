package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
data object RestoreSelectCoinsScreen :  RestoreAccountChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()

        ManageWalletsScreen(
            mainViewModel = mainViewModel,
            openBirthdayHeightConfigure = { token ->
                when (token.blockchainType) {
                    BlockchainType.Zcash -> backStack.add(ZcashConfigureScreen)
                    BlockchainType.Monero -> backStack.add(MoneroConfigureScreen)
                    else -> Unit
                }
            },
            onBackClick = {
                backStack.removeLastOrNull()
            }
        ) {
//            TODO("xxx nav3")
//            fragmentNavController.popBackStack(popUpToInclusiveId, inclusive)
        }
    }
}