package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

class ReceiveChooseCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Serializable
data object ReceiveChooseCoinScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val activeAccount = App.accountManager.activeAccount
        if (activeAccount == null) {
            CloseWithMessage(backStack)
            return
        }
        ReceiveTokenSelectScreen(
            activeAccount = activeAccount,
            onMultipleAddressesClick = { coinUid ->
                viewModel.coinUid = coinUid
                backStack.add(ReceiveBchAddressFormatScreen)
            },
            onMultipleDerivationsClick = { coinUid ->
                viewModel.coinUid = coinUid
                backStack.add(ReceiveDerivationSelectScreen)
            },
            onMultipleBlockchainsClick = { coinUid ->
                viewModel.coinUid = coinUid
                backStack.add(ReceiveNetworkSelectScreen)
            },
            onMultipleZcashAddressTypeClick = { wallet ->
                viewModel.wallet = wallet
                backStack.add(ReceiveZcashAddressTypeSelectScreen)
            },
            onCoinClick = { wallet ->
                onSelectWallet(wallet, backStack)
            },
            onBackPress = { backStack.removeLastOrNull() },
        )
    }
}

fun onSelectWallet(
    wallet: Wallet,
    backStack: NavBackStack<HSScreen>,
    isTransparentAddress: Boolean = false,
) {
    backStack.add(
        ReceiveScreen(
            wallet,
            ReceiveChooseCoinScreen::class,
            isTransparentAddress
        )
    )

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}

@Composable
fun CloseWithMessage(backStack: NavBackStack<HSScreen>) {
    val view = LocalView.current
    HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
    backStack.closeModule()
}

fun NavBackStack<HSScreen>.closeModule() {
    removeLastUntil(ReceiveChooseCoinScreen::class, true)
}
