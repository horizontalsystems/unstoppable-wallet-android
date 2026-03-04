package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import kotlinx.serialization.Serializable

@Serializable
data object RestorePhraseNonStandardScreen :  RestoreAccountChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()

        RestorePhraseNonStandard(
            mainViewModel = mainViewModel,
            openSelectCoinsScreen = { backStack.add(RestoreSelectCoinsScreen) },
            onBackClick = { backStack.removeLastOrNull() }
        )
    }
}