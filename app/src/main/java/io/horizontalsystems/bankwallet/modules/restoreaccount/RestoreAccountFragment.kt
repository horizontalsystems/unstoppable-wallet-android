package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
data class RestoreAccountScreen(
    val popOffOnSuccess: Int,
    val popOffInclusive: Boolean
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val popUpToInclusiveId = popOffOnSuccess ?: R.id.restoreAccountFragment
        val inclusive = popOffInclusive ?: false

        val restoreMenuViewModel = viewModel<RestoreMenuViewModel>(factory = RestoreMenuModule.Factory())
        val mainViewModel = viewModel<RestoreViewModel>()

        RestorePhrase(
            advanced = false,
            restoreMenuViewModel = restoreMenuViewModel,
            mainViewModel = mainViewModel,
            openRestoreAdvanced = { backStack.add(restore_phrase_advanced) },
            openSelectCoins = { backStack.add(restore_select_coins) },
            openNonStandardRestore = { backStack.add(restore_phrase_nonstandard) },
            onBackClick = { backStack.removeLastOrNull() },
        )
    }
}

abstract class RestoreAccountScreenChild : HSScreen() {
    override fun getParentVMKey(backStack: NavBackStack<HSScreen>): String? {
        return backStack.findLast { it is RestoreAccountScreen }?.toString()
    }
}

@Serializable
data object restore_phrase_advanced : RestoreAccountScreenChild() {
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
            openSelectCoinsScreen = { backStack.add(restore_select_coins) },
            openNonStandardRestore = {
                backStack.add(restore_phrase_nonstandard)

                stat(
                    page = StatPage.ImportWalletFromKeyAdvanced,
                    event = StatEvent.Open(StatPage.ImportWalletNonStandard)
                )
            },
            onBackClick = { backStack.removeLastOrNull() }
        )
    }
}

@Serializable
data object restore_select_coins :  RestoreAccountScreenChild() {
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
                    BlockchainType.Zcash -> backStack.add(zcash_configure)
                    BlockchainType.Monero -> backStack.add(monero_configure)
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

data object restore_phrase_nonstandard :  RestoreAccountScreenChild() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()

        RestorePhraseNonStandard(
            mainViewModel = mainViewModel,
            openSelectCoinsScreen = { backStack.add(restore_select_coins) },
            onBackClick = { backStack.removeLastOrNull() }
        )
    }
}

data object zcash_configure :  RestoreAccountScreenChild() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
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

data object monero_configure :  RestoreAccountScreenChild() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()

        RestoreBirthdayHeightScreen(
            blockchainType = BlockchainType.Monero,
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

class RestoreAccountFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
    }

}
