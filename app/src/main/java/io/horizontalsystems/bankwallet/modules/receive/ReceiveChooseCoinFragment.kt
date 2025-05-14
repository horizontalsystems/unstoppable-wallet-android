package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveChooseCoinRoutes.BCH_ADDRESS_FORMAT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveChooseCoinRoutes.COIN_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveChooseCoinRoutes.DERIVATION_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveChooseCoinRoutes.NETWORK_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.NetworkSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.BchAddressTypeSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel

class ReceiveChooseCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        ReceiveChooseCoinScreen(navController)
    }
}

object ReceiveChooseCoinRoutes {
    const val COIN_SELECT_SCREEN = "coin_select_screen"
    const val BCH_ADDRESS_FORMAT_SCREEN = "bch_address_format_screen"
    const val DERIVATION_SELECT_SCREEN = "derivation_select_screen"
    const val NETWORK_SELECT_SCREEN = "network_select_screen"
}

@Composable
fun ReceiveChooseCoinScreen(
    fragmentNavController: NavController
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "receive_screen_choose_coin"
    ) {
        navigation(
            startDestination = COIN_SELECT_SCREEN,
            route = "receive_screen_choose_coin"
        ) {
            composablePage(COIN_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val activeAccount = App.accountManager.activeAccount
                if (activeAccount == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                ReceiveTokenSelectScreen(
                    activeAccount = activeAccount,
                    onMultipleAddressesClick = { coinUid ->
                        viewModel.coinUid = coinUid
                        navController.navigate(BCH_ADDRESS_FORMAT_SCREEN)
                    },
                    onMultipleDerivationsClick = { coinUid ->
                        viewModel.coinUid = coinUid
                        navController.navigate(DERIVATION_SELECT_SCREEN)
                    },
                    onMultipleBlockchainsClick = { coinUid ->
                        viewModel.coinUid = coinUid
                        navController.navigate(NETWORK_SELECT_SCREEN)
                    },
                    onCoinClick = { wallet ->
                        onSelectWallet(wallet, fragmentNavController)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController),
                )
            }
            composablePage(BCH_ADDRESS_FORMAT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val coinUid = viewModel.coinUid
                if (coinUid == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                val bchAddressViewModel = viewModel<BchAddressTypeSelectViewModel>(
                    factory = BchAddressTypeSelectViewModel.Factory(coinUid)
                )
                AddressFormatSelectScreen(
                    addressFormatItems = bchAddressViewModel.items,
                    description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
                    onSelect = { wallet ->
                        onSelectWallet(wallet, fragmentNavController)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController)
                )
            }
            composablePage(DERIVATION_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val coinUid = viewModel.coinUid
                if (coinUid == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                val derivationViewModel = viewModel<DerivationSelectViewModel>(
                    factory = DerivationSelectViewModel.Factory(coinUid)
                )
                AddressFormatSelectScreen(
                    addressFormatItems = derivationViewModel.items,
                    description = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
                    onSelect = { wallet ->
                        onSelectWallet(wallet, fragmentNavController)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController)
                )
            }
            composablePage(NETWORK_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val activeAccount = viewModel.activeAccount
                val fullCoin = viewModel.fullCoin()
                if (activeAccount == null || fullCoin == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                NetworkSelectScreen(
                    navController = navController,
                    activeAccount = activeAccount,
                    fullCoin = fullCoin,
                    onSelect = { wallet ->
                        onSelectWallet(wallet, fragmentNavController)
                    }
                )
            }
        }
    }
}

private fun onSelectWallet(wallet: Wallet, fragmentNavController: NavController) {
    fragmentNavController.slideFromRight(R.id.receiveFragment, wallet)

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}
