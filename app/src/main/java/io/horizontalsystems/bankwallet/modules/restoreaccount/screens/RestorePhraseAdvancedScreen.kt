package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RestorePhraseAdvancedScreen : RestoreAccountChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val restoreMenuViewModel = viewModel<RestoreMenuViewModel>()
        val mainViewModel = viewModel<RestoreViewModel>()

        AdvancedRestoreScreen(
            restoreMenuViewModel = restoreMenuViewModel,
            mainViewModel = mainViewModel,
            openSelectCoinsScreen = { backStack.add(RestoreSelectCoinsScreen) },
            openNonStandardRestore = {
                backStack.add(RestorePhraseNonStandardScreen)

                stat(
                    page = StatPage.ImportWalletFromKeyAdvanced,
                    event = StatEvent.Open(StatPage.ImportWalletNonStandard)
                )
            },
            onBackClick = { backStack.removeLastOrNull() }
        )
    }
}