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
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.NetworkSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.BchAddressTypeSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

@Serializable
abstract class ReceiveChooseCoinChildScreen : HSScreen(
    parentScreenClass = ReceiveChooseCoinScreen::class
)

class ReceiveChooseCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Serializable
data object ReceiveChooseCoinScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
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
                backStack.add(bch_address_format_screen)
            },
            onMultipleDerivationsClick = { coinUid ->
                viewModel.coinUid = coinUid
                backStack.add(derivation_select_screen)
            },
            onMultipleBlockchainsClick = { coinUid ->
                viewModel.coinUid = coinUid
                backStack.add(network_select_screen)
            },
            onMultipleZcashAddressTypeClick = { wallet ->
                viewModel.wallet = wallet
                backStack.add(zcash_address_type_select_screen)
            },
            onCoinClick = { wallet ->
                onSelectWallet(wallet, backStack)
            },
            onBackPress = { backStack.removeLastOrNull() },
        )
    }
}

@Serializable
data object bch_address_format_screen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(backStack)
            return
        }
        val bchAddressViewModel = viewModel<BchAddressTypeSelectViewModel>(
            factory = BchAddressTypeSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = bchAddressViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
            onSelect = { wallet ->
                onSelectWallet(wallet, backStack)
            },
            closeModule = { backStack.closeModule() },
            onBackPress = { backStack.removeLastOrNull() }
        )
    }
}

@Serializable
data object derivation_select_screen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(backStack)
            return
        }
        val derivationViewModel = viewModel<DerivationSelectViewModel>(
            factory = DerivationSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = derivationViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
            onSelect = { wallet ->
                onSelectWallet(wallet, backStack)
            },
            closeModule = { backStack.closeModule() },
            onBackPress = { backStack.removeLastOrNull() }
        )
    }
}

@Serializable
data object network_select_screen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val activeAccount = viewModel.activeAccount
        val fullCoin = viewModel.fullCoin()
        if (activeAccount == null || fullCoin == null) {
            CloseWithMessage(backStack)
            return
        }
        NetworkSelectScreen(
            backStack = backStack,
            activeAccount = activeAccount,
            fullCoin = fullCoin,
            closeModule = { backStack.closeModule() },
            onSelect = { wallet ->
                onSelectWallet(wallet, backStack)
            }
        )
    }
}

@Serializable
data object zcash_address_type_select_screen : ReceiveChooseCoinChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val wallet = viewModel.wallet
        if (wallet == null) {
            CloseWithMessage(backStack)
            return
        }

        ZcashAddressTypeSelectScreen(
            onZcashAddressTypeClick = { isTransparent ->
                onSelectWallet(wallet, backStack, isTransparent)
            },
            onBackPress = { backStack.removeLastOrNull() },
            closeModule = { backStack.closeModule() }
        )
    }
}

private fun onSelectWallet(
    wallet: Wallet,
    backStack: NavBackStack<HSScreen>,
    isTransparentAddress: Boolean = false,
) {
    backStack.add(
        ReceiveScreen(
            wallet,
            R.id.receiveChooseCoinFragment,
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

private fun NavBackStack<HSScreen>.closeModule() {
    removeLastUntil(ReceiveChooseCoinScreen::class, true)
}
