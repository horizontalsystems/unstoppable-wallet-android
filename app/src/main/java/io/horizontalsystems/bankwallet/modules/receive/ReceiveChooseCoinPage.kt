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
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<ReceiveSharedViewModel>()
        val activeAccount = App.accountManager.activeAccount
        if (activeAccount == null) {
            CloseWithMessage(navigation)
            return
        }
        ReceiveTokenSelectScreen(
            activeAccount = activeAccount,
            onMultipleAddressesClick = { coinUid ->
                viewModel.coinUid = coinUid
                navigation.add(BchAddressFormatPage)
            },
            onMultipleDerivationsClick = { coinUid ->
                viewModel.coinUid = coinUid
                navigation.add(DerivationSelectPage)
            },
            onMultipleBlockchainsClick = { coinUid ->
                viewModel.coinUid = coinUid
                navigation.add(NetworkSelectPage)
            },
            onMultipleZcashAddressTypeClick = { wallet ->
                navigation.add(
                    ZcashAddressTypeSelectPage(
                        ZcashAddressTypeSelectPage.Input(wallet, ReceiveChooseCoinPage::class)
                    )
                )
            },
            onCoinClick = { wallet ->
                onSelectWallet(wallet, navigation)
            },
            onBackPress = { navigation.removeLastOrNull() },
        )
    }
}

@Serializable
data object BchAddressFormatPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(navigation)
            return
        }
        val bchAddressViewModel = viewModel<BchAddressTypeSelectViewModel>(
            factory = BchAddressTypeSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = bchAddressViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
            onSelect = { wallet ->
                onSelectWallet(wallet, navigation)
            },
            closeModule = { navigation.removeLastOrNull() },
            onBackPress = { navigation.removeLastOrNull() }
        )
    }
}

data object DerivationSelectPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val coinUid = viewModel.coinUid
        if (coinUid == null) {
            CloseWithMessage(navigation)
            return
        }
        val derivationViewModel = viewModel<DerivationSelectViewModel>(
            factory = DerivationSelectViewModel.Factory(coinUid)
        )
        AddressFormatSelectScreen(
            addressFormatItems = derivationViewModel.items,
            description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
            onSelect = { wallet ->
                onSelectWallet(wallet, navigation)
            },
            closeModule = { navigation.removeLastOrNull() },
            onBackPress = { navigation.removeLastOrNull() }
        )
    }
}

data object NetworkSelectPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<ReceiveSharedViewModel>(ReceiveChooseCoinPage::class)
        val activeAccount = viewModel.activeAccount
        val fullCoin = viewModel.fullCoin()
        if (activeAccount == null || fullCoin == null) {
            CloseWithMessage(navigation)
            return
        }
        NetworkSelectScreen(
            navigation = navigation,
            activeAccount = activeAccount,
            fullCoin = fullCoin,
            closeModule = { navigation.removeLastOrNull() },
            onSelect = { wallet ->
                onSelectWallet(wallet, navigation)
            }
        )
    }
}

private fun onSelectWallet(
    wallet: Wallet,
    navigation: HSNavigation,
    isTransparentAddress: Boolean = false,
) {
    navigation.slideFromRight(
        ReceivePage(ReceivePage.Input(
            wallet,
            ReceiveChooseCoinPage::class,
            isTransparentAddress
        ))
    )

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}

@Composable
fun CloseWithMessage(navigation: HSNavigation) {
    val view = LocalView.current
    HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
    navigation.removeLastOrNull()
}