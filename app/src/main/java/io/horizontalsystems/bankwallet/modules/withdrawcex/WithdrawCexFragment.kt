package io.horizontalsystems.bankwallet.modules.withdrawcex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexWithdrawNetwork
import io.horizontalsystems.bankwallet.core.providers.ICexProvider
import io.horizontalsystems.bankwallet.modules.settings.about.*
import io.horizontalsystems.bankwallet.modules.withdrawcex.ui.WithdrawCexConfirmScreen
import io.horizontalsystems.bankwallet.modules.withdrawcex.ui.WithdrawCexScreen
import io.horizontalsystems.bankwallet.modules.withdrawcex.ui.WithdrawCexSecurityVerificationScreen
import io.horizontalsystems.bankwallet.modules.withdrawcex.ui.WithdrawCexSelectNetworkScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

class WithdrawCexFragment : BaseFragment() {

    companion object {
        fun args(cexAsset: CexAsset) = bundleOf("cexAsset" to cexAsset)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cexAsset = arguments?.getParcelable<CexAsset>("cexAsset")
        val network = cexAsset?.withdrawNetworks?.find { it.isDefault }
        val cexProvider = App.cexProviderManager.cexProviderFlow.value

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    val navController = findNavController()

                    if (cexAsset != null && network != null && cexProvider != null) {
                        WithdrawCexNavHost(navController, cexAsset, network, cexProvider)
                    } else {
                        val view = LocalView.current
                        HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))

                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WithdrawCexNavHost(
    fragmentNavController: NavController,
    cexAsset: CexAsset,
    network: CexWithdrawNetwork,
    cexProvider: ICexProvider,
) {
    val viewModel: WithdrawCexViewModel = viewModel(factory = WithdrawCexModule.Factory(cexAsset, network, cexProvider))
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "withdraw",
    ) {
        composable("withdraw") {
            WithdrawCexScreen(
                mainViewModel = viewModel,
                fragmentNavController = fragmentNavController,
                onClose = { fragmentNavController.popBackStack() },
                openNetworkSelect = {
                    navController.navigate("withdraw-select-network")
                },
                openConfirm = {
                    navController.navigate("withdraw-confirm")
                },
            )
        }
        composablePopup("withdraw-select-network") {
            WithdrawCexSelectNetworkScreen(
                mainViewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composablePage("withdraw-confirm") {
            WithdrawCexConfirmScreen(
                mainViewModel = viewModel,
                openVerification = {
                    navController.navigate("withdraw-verification/$it")
                },
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("withdraw-verification/{withdrawId}") { backStackEntry ->
            val withdrawId = backStackEntry.arguments?.getString("withdrawId")

            withdrawId?.let {
                WithdrawCexSecurityVerificationScreen(
                    withdrawId = withdrawId,
                    onNavigateBack = { navController.popBackStack() }
                ) { fragmentNavController.popBackStack() }
            }

        }
    }
}
