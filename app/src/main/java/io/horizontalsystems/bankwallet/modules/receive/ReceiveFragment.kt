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
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.activatetoken.ActivateTokenFragment
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.RECEIVE_ADDRESS_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ReceiveRoutes.USED_ADDRESSES_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveAddressViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveSharedViewModel
import io.horizontalsystems.core.helpers.HudHelper

class ReceiveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Wallet>(navController) {
            ReceiveScreen(it, navController)
        }
    }
}

object ReceiveRoutes {
    const val RECEIVE_ADDRESS_SCREEN = "receive_address_screen"
    const val USED_ADDRESSES_SCREEN = "used_addresses_screen"
}

@Composable
fun ReceiveScreen(
    wallet: Wallet,
    fragmentNavController: NavController
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "receive_screen"
    ) {
        navigation(
            startDestination = RECEIVE_ADDRESS_SCREEN,
            route = "receive_screen"
        ) {
            composablePage(RECEIVE_ADDRESS_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<ReceiveSharedViewModel>(navController)

                val addressViewModel =
                    viewModel<ReceiveAddressViewModel>(factory = ReceiveModule.Factory(wallet))
                val context = LocalContext.current

                ReceiveAddressScreen(
                    title = stringResource(R.string.Deposit_Title, wallet.coin.code),
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
                        viewModel.usedAddressesParams = UsedAddressesParams(
                            wallet.coin.name,
                            usedAddresses,
                            usedChangeAddresses
                        )
                        navController.navigate(USED_ADDRESSES_SCREEN)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController),
                    closeModule = { fragmentNavController.popBackStack() },
                    onClickActivate = {
                        fragmentNavController.slideFromBottomForResult<ActivateTokenFragment.Result>(
                            R.id.activateTokenFragment,
                            wallet
                        ) {
                            addressViewModel.onActivatedResult(it.activated)
                        }
                    }
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
        }
    }
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