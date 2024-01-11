package io.horizontalsystems.bankwallet.modules.depositcex

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.modules.depositcex.ReceiveRoutes.ASSET_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.depositcex.ReceiveRoutes.DEPOSIT_SCREEN
import io.horizontalsystems.bankwallet.modules.depositcex.ReceiveRoutes.NETWORK_SELECT_SCREEN
import io.horizontalsystems.bankwallet.modules.depositcex.ReceiveRoutes.USED_ADDRESS_SCREEN
import io.horizontalsystems.bankwallet.modules.receive.CloseWithMessage
import io.horizontalsystems.bankwallet.modules.receive.navigateBack
import io.horizontalsystems.bankwallet.modules.receive.sharedViewModel
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams

class DepositCexFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<CexAsset>()
        CexDepositScreen(input, navController)
    }
}

object ReceiveRoutes {
    const val DEPOSIT_SCREEN = "deposit_screen"
    const val ASSET_SELECT_SCREEN = "asset_select_screen"
    const val NETWORK_SELECT_SCREEN = "network_select_screen"
    const val USED_ADDRESS_SCREEN = "used_address_screen"
}

@Composable
fun CexDepositScreen(
    asset: CexAsset?,
    fragmentNavController: NavController
) {
    val startDestination = if (asset == null) {
        ASSET_SELECT_SCREEN
    } else if (asset.depositNetworks.isEmpty() || asset.depositNetworks.size == 1) {
        DEPOSIT_SCREEN
    } else {
        NETWORK_SELECT_SCREEN
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "deposit_address"
    ) {
        navigation(
            startDestination = startDestination,
            route = "deposit_address"
        ) {
            composablePage(DEPOSIT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<CexDepositSharedViewModel>(navController)
                val cexAsset = asset ?: viewModel.cexAsset
                if (cexAsset == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }

                val addressViewModel =
                    viewModel<DepositAddressViewModel>(factory = DepositAddressViewModel.Factory(cexAsset, viewModel.network))
                val context = LocalContext.current

                ReceiveAddressScreen(
                    title = stringResource(R.string.CexDeposit_Title, cexAsset.id),
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
                        viewModel.usedAddressesParams = UsedAddressesParams(cexAsset.name, usedAddresses, usedChangeAddresses)
                        navController.navigate(USED_ADDRESS_SCREEN)
                    },
                    onBackPress = navigateBack(fragmentNavController, navController),
                    closeModule = { fragmentNavController.popBackStack() },
                )
            }
            composablePage(USED_ADDRESS_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<CexDepositSharedViewModel>(navController)
                val usedAddressesParams = viewModel.usedAddressesParams
                if (usedAddressesParams == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                UsedAddressScreen(usedAddressesParams) { navController.popBackStack() }
            }
            composablePage(ASSET_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<CexDepositSharedViewModel>(navController)
                SelectCoinScreen(
                    onClose = navigateBack(fragmentNavController, navController),
                    itemIsSuspended = { !it.depositEnabled },
                    onSelectAsset = { cexAsset ->
                        viewModel.cexAsset = cexAsset
                        if (cexAsset.depositNetworks.isEmpty() || cexAsset.depositNetworks.size == 1) {
                            viewModel.network = cexAsset.depositNetworks.firstOrNull()
                            navController.navigate(DEPOSIT_SCREEN)
                        } else {
                            navController.navigate(NETWORK_SELECT_SCREEN)
                        }
                    },
                    withBalance = false
                )
            }
            composablePage(NETWORK_SELECT_SCREEN) { entry ->
                val viewModel = entry.sharedViewModel<CexDepositSharedViewModel>(navController)
                val cexAsset = asset ?: viewModel.cexAsset
                if (cexAsset == null) {
                    CloseWithMessage(fragmentNavController)
                    return@composablePage
                }
                SelectNetworkScreen(
                    networks = cexAsset.depositNetworks,
                    onNavigateBack = navigateBack(fragmentNavController, navController),
                    onSelectNetwork = {
                        viewModel.network = it
                        navController.navigate(DEPOSIT_SCREEN)
                    }
                )
            }
        }
    }
}