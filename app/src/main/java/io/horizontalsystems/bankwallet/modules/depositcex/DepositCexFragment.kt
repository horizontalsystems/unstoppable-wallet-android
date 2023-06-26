package cash.p.terminal.modules.depositcex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

class DepositCexFragment : BaseFragment() {

    companion object {
        fun args(cexAsset: CexAsset): Bundle {
            return bundleOf("cexAsset" to cexAsset)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val cexAsset = arguments?.getParcelable<CexAsset>("cexAsset")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    val navController = findNavController()

                    if (cexAsset != null) {
                        DepositQrCodeScreen(
                            cexAsset = cexAsset,
                            onNavigateBack = { navController.popBackStack() },
                            onClose = { navController.popBackStack() },
                        )
                    } else {
                        val view = LocalView.current
                        HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
                        navController.popBackStack()
                    }
                }
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
