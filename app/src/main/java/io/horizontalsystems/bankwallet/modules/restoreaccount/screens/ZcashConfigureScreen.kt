package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
data object ZcashConfigureScreen :  RestoreAccountChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()

        RestoreBirthdayHeightScreen(
            blockchainType = BlockchainType.Zcash,
            onCloseWithResult = { config ->
                mainViewModel.setBirthdayHeightConfig(config)
                backStack.removeLastOrNull()
            },
            onCloseClick = {
                mainViewModel.cancelBirthdayHeightConfig = true
                backStack.removeLastOrNull()
            }
        )
    }
}