package io.horizontalsystems.bankwallet.modules.receive

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.BCH_ADDRESS_FORMAT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.COIN_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.DERIVATION_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.NETWORK_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.RECEIVE_ADDRESS_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.USED_ADDRESSES_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.NetworkSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.BchAddressTypeSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.DerivationSelectViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveAddressViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import io.horizontalsystems.core.helpers.HudHelper

class ReceiveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val wallet = navController.getInput<Wallet>()
        ReceiveScreen(wallet, navController)
    }
}

object ReceiveRoutes {
    const val RECEIVE_ADDRESS_SCREEN = "receive_address_screen"
    const val COIN_SELECT_SCREEN = "coin_select_screen"
    const val USED_ADDRESSES_SCREEN = "used_addresses_screen"
    const val BCH_ADDRESS_FORMAT_SCREEN = "bch_address_format_screen"
    const val DERIVATION_SELECT_SCREEN = "derivation_select_screen"
    const val NETWORK_SELECT_SCREEN = "network_select_screen"
}

@Composable
fun ReceiveScreen(
    wallet: Wallet?,
    fragmentNavController: NavController
) {
    val startDestination = if (wallet != null) RECEIVE_ADDRESS_SCREEN else COIN_SELECT_SCREEN
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "receive_screen"
    ) {
        navigation(
            startDestination = startDestination,
            route = "receive_screen"
        ) {
            composablePage(RECEIVE_ADDRESS_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)

                val walletNonNull = wallet ?: viewModel.wallet
                if (walletNonNull == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }

                val addressViewModel = viewModel<ReceiveAddressViewModel>(factory = ReceiveModule.Factory(walletNonNull))
                val context = LocalContext.current

                ReceiveAddressScreen(
                    title = stringResource(R.string.Deposit_Title, walletNonNull.coin.code),
                    uiState = addressViewModel.uiState,
                    onErrorClick = { addressViewModel.onErrorClick() },
                    setAmount = { amount -> addressViewModel.setAmount(amount) },
                    onShareClick = { address ->
                        context.startActivity(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, address)
                            type = "text/plain"
                        })
                    },
                    showUsedAddresses = { usedAddresses, usedChangeAddresses ->
                        viewModel.usedAddressesParams = UsedAddressesParams(walletNonNull.coin.name, usedAddresses, usedChangeAddresses)
                        navController.navigate(USED_ADDRESSES_SCREEN)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController),
                    closeModule = { fragmentNavController.popBackStack() }
                )
            }
            composablePage(USED_ADDRESSES_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)
                val usedAddressesParams = viewModel.usedAddressesParams
                if (usedAddressesParams == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                UsedAddressScreen(usedAddressesParams) { navController.popBackStack() }
            }
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
                        onSelectWallet(wallet, viewModel, navController)
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
                        onSelectWallet(wallet, viewModel, navController)
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
                        onSelectWallet(wallet, viewModel, navController)
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
                        onSelectWallet(wallet, viewModel, navController)
                    }
                )
            }
        }
    }
}

private fun onSelectWallet(wallet: Wallet, viewModel: ReceiveSharedViewModel, navController: NavController) {
    viewModel.wallet = wallet
    navController.navigate(RECEIVE_ADDRESS_SCREEN)

    stat(page = StatPage.ReceiveTokenList, event = StatEvent.OpenReceive(wallet.token))
}

@Composable
fun CloseWithMessage(navController: NavController) {
    val view = LocalView.current
    HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
    navController.popBackStack()
}

fun navigateBack(fragmentNavController: NavController, navController: NavHostController): () -> Unit = {
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