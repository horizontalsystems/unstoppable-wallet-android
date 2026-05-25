package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.NetworkSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.BchAddressTypeSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

@Serializable
data object ReceiveChooseCoinPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val activeAccount = App.accountManager.activeAccount
        if (activeAccount == null) {
            CloseWithMessage(navController)
            return
        }
        ReceiveTokenSelectScreen(
            activeAccount = activeAccount,
            onMultipleAddressesClick = { coinUid ->
                viewModel.coinUid = coinUid
                navController.add(BchAddressFormatPage)
            },
            onMultipleDerivationsClick = { coinUid ->
                viewModel.coinUid = coinUid
                navController.add(DerivationSelectPage)
            },
            onMultipleBlockchainsClick = { coinUid ->
                viewModel.coinUid = coinUid
                navController.add(NetworkSelectPage)
            },
            onMultipleZcashAddressTypeClick = { wallet ->
                navController.add(
                    ZcashAddressTypeSelectPage(
                        ZcashAddressTypeSelectPage.Input(wallet, ReceiveChooseCoinPage::class)
                    )
                )
            },
            onCoinClick = { wallet ->
                onSelectWallet(wallet, navController)
            },
            onBackPress = { navController.removeLastOrNull() },
        )
    }
}

@Serializable
data object BchAddressFormatPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(navController)
            return
        }
        val bchAddressViewModel = viewModel<BchAddressTypeSelectViewModel>(
            factory = BchAddressTypeSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = bchAddressViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
            onSelect = { wallet ->
                onSelectWallet(wallet, navController)
            },
            closeModule = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() }
        )
    }
}

data object DerivationSelectPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(navController)
            return
        }
        val derivationViewModel = viewModel<DerivationSelectViewModel>(
            factory = DerivationSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = derivationViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
            onSelect = { wallet ->
                onSelectWallet(wallet, navController)
            },
            closeModule = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() }
        )
    }
}

data object NetworkSelectPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val activeAccount = viewModel.activeAccount
        val fullCoin = viewModel.fullCoin()
        if (activeAccount == null || fullCoin == null) {
            CloseWithMessage(navController)
            return
        }
        NetworkSelectScreen(
            navController = navController,
            activeAccount = activeAccount,
            fullCoin = fullCoin,
            closeModule = { navController.removeLastOrNull() },
            onSelect = { wallet ->
                onSelectWallet(wallet, navController)
            }
        )
    }
}

private fun onSelectWallet(
    wallet: Wallet,
    fragmentNavController: HSNavigation,
    isTransparentAddress: Boolean = false,
) {
    fragmentNavController.slideFromRight(
        ReceivePage(ReceivePage.Input(
            wallet,
            ReceiveChooseCoinPage::class,
            isTransparentAddress
        ))
    )

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}

@Composable
fun CloseWithMessage(navController: HSNavigation) {
    val view = LocalView.current
    HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
    navController.removeLastOrNull()
}