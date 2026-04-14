package com.quantum.wallet.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.composablePage
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.receive.ReceiveChooseCoinRoutes.BCH_ADDRESS_FORMAT_SCREEN
import com.quantum.wallet.bankwallet.modules.receive.ReceiveChooseCoinRoutes.COIN_SELECT_SCREEN
import com.quantum.wallet.bankwallet.modules.receive.ReceiveChooseCoinRoutes.DERIVATION_SELECT_SCREEN
import com.quantum.wallet.bankwallet.modules.receive.ReceiveChooseCoinRoutes.NETWORK_SELECT_SCREEN
import com.quantum.wallet.bankwallet.modules.receive.ReceiveChooseCoinRoutes.ZCASH_ADDRESS_TYPE_SELECT_SCREEN
import com.quantum.wallet.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import com.quantum.wallet.bankwallet.modules.receive.ui.NetworkSelectScreen
import com.quantum.wallet.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import com.quantum.wallet.bankwallet.modules.receive.viewmodels.BchAddressTypeSelectViewModel
import com.quantum.wallet.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import com.quantum.wallet.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import com.quantum.wallet.core.helpers.HudHelper

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
    const val ZCASH_ADDRESS_TYPE_SELECT_SCREEN = "zcash_address_type_select_screen"
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
                    onMultipleZcashAddressTypeClick = { wallet ->
                        viewModel.wallet = wallet
                        navController.navigate(ZCASH_ADDRESS_TYPE_SELECT_SCREEN)
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
                    closeModule = { fragmentNavController.popBackStack() },
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
                    closeModule = { fragmentNavController.popBackStack() },
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
                    closeModule = { fragmentNavController.popBackStack() },
                    onSelect = { wallet ->
                        onSelectWallet(wallet, fragmentNavController)
                    }
                )
            }
            composablePage(ZCASH_ADDRESS_TYPE_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val wallet = viewModel.wallet
                if (wallet == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }

                ZcashAddressTypeSelectScreen(
                    onZcashAddressTypeClick = { isTransparent ->
                        onSelectWallet(wallet, fragmentNavController, isTransparent)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController),
                    closeModule = { fragmentNavController.popBackStack() }
                )
            }
        }
    }
}

private fun onSelectWallet(
    wallet: Wallet,
    fragmentNavController: NavController,
    isTransparentAddress: Boolean = false,
) {
    fragmentNavController.slideFromRight(
        R.id.receiveFragment,
        ReceiveFragment.Input(
            wallet,
            R.id.receiveChooseCoinFragment,
            isTransparentAddress
        )
    )

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}

fun navigateBack(
    fragmentNavController: NavController,
    navController: NavHostController
): () -> Unit = {
    val result = navController.popBackStack()
    if (!result) {
        fragmentNavController.popBackStack()
    }
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry)
}

@Composable
fun CloseWithMessage(navController: NavController) {
    val view = LocalView.current
    HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
    navController.popBackStack()
}