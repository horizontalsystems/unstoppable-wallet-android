package io.horizontalsystems.bankwallet.modules.depositcex

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexDepositNetwork
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable

class DepositCexFragment : BaseComposeFragment() {

    companion object {
        fun args(cexAsset: CexAsset, network: CexDepositNetwork? = null): Bundle {
            return bundleOf(
                "cexAsset" to cexAsset,
                "cexDepositNetwork" to network,
            )
        }
    }

    @Composable
    override fun GetContent() {
        val cexAsset = arguments?.parcelable<CexAsset>("cexAsset")
        val network = arguments?.parcelable<CexDepositNetwork>("cexDepositNetwork")
        ComposeAppTheme {
            val navController = findNavController()
            val navigatedFromMain = navController.previousBackStackEntry?.destination?.id == R.id.mainFragment
            val navigateBack: () -> Unit = { navController.popBackStack() }

            if (cexAsset != null) {
                val networks = cexAsset.depositNetworks
                if (networks.isEmpty() || network != null || networks.size == 1) {
                    DepositQrCodeScreen(
                        cexAsset = cexAsset,
                        onNavigateBack = if (navigatedFromMain) null else navigateBack,
                        onClose = { navController.popBackStack(R.id.mainFragment, false) },
                        network = network ?: networks.firstOrNull()
                    )
                } else {
                    SelectNetworkScreen(
                        networks = networks,
                        onNavigateBack = if (navigatedFromMain) null else navigateBack,
                        onClose = { navController.popBackStack(R.id.mainFragment, false) },
                        onSelectNetwork = {
                            navController.slideFromRight(R.id.depositCexFragment, args(cexAsset, it))
                        }
                    )
                }

            } else {
                val view = LocalView.current
                HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
                navController.popBackStack()
            }
        }
    }

}

//@OptIn(ExperimentalAnimationApi::class)
//@Composable
//fun DepositCexNavHost(
//    fragmentNavController: NavController,
//    assetId: String,
//) {
//    val depositViewModel = viewModel<DepositViewModel>(factory = DepositCexModule.Factory(assetId))
//    val navController = rememberAnimatedNavController()
//
//    AnimatedNavHost(
//        navController = navController,
//        startDestination = "deposit-qrcode",
//    ) {
//        composablePage("select-network") {
//            SelectNetworkScreen(
//                depositViewModel = depositViewModel,
//                openQrCode = { navController.navigate("deposit-qrcode") },
//                onClose = { fragmentNavController.popBackStack() },
//            )
//        }
//
//        composablePage("deposit-qrcode") {
//            DepositQrCodeScreen(
//                depositViewModel = depositViewModel,
//                onNavigateBack = { navController.popBackStack() },
//                onClose = { fragmentNavController.popBackStack() },
//            )
//        }
//    }
//}
