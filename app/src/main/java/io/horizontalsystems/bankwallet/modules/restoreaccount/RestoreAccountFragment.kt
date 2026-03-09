package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreaccount.screens.RestorePhraseAdvancedScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.screens.RestorePhraseNonStandardScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.screens.RestoreSelectCoinsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreAccountScreen(
    val popOffOnSuccess: KClass<out HSScreen>,
    val popOffInclusive: Boolean
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val restoreMenuViewModel = viewModel<RestoreMenuViewModel>(factory = RestoreMenuModule.Factory())
        val mainViewModel = viewModel<RestoreViewModel>()

        RestorePhrase(
            advanced = false,
            restoreMenuViewModel = restoreMenuViewModel,
            mainViewModel = mainViewModel,
            openRestoreAdvanced = { backStack.add(RestorePhraseAdvancedScreen(popOffOnSuccess, popOffInclusive)) },
            openSelectCoins = { backStack.add(RestoreSelectCoinsScreen(popOffOnSuccess, popOffInclusive)) },
            openNonStandardRestore = { backStack.add(RestorePhraseNonStandardScreen(popOffOnSuccess, popOffInclusive)) },
            onBackClick = { backStack.removeLastOrNull() },
        )
    }
}

class RestoreAccountFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
    }

}
