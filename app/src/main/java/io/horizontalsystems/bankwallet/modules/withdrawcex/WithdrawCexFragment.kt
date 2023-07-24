package cash.p.terminal.modules.withdrawcex

import android.os.Bundle
import android.util.Log
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
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.core.providers.BinanceCexProvider
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexWithdrawNetwork
import cash.p.terminal.core.providers.CoinzixCexProvider
import cash.p.terminal.modules.coinzixverify.CoinzixVerificationViewModel
import cash.p.terminal.modules.coinzixverify.TwoFactorType
import cash.p.terminal.modules.coinzixverify.ui.CoinzixVerificationScreen
import cash.p.terminal.modules.settings.about.*
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexConfirmScreen
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexScreen
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexSelectNetworkScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*
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
        val network = cexAsset?.withdrawNetworks?.find { it.isDefault } ?: cexAsset?.withdrawNetworks?.firstOrNull()
        val cexProvider = App.cexProviderManager.cexProviderFlow.value

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    val navController = findNavController()

                    if (cexAsset != null && network != null && cexProvider != null) {
                        when (cexProvider) {
                            is BinanceCexProvider -> {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            }

                            is CoinzixCexProvider -> {
                                CoinzixWithdrawNavHost(navController, cexAsset, network, cexProvider)
                            }
                        }

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
fun CoinzixWithdrawNavHost(
    fragmentNavController: NavController,
    cexAsset: CexAsset,
    network: CexWithdrawNetwork,
    coinzixCexProvider: CoinzixCexProvider,
) {
    val viewModel: WithdrawCexViewModel = viewModel(factory = WithdrawCexModule.Factory(cexAsset, network, coinzixCexProvider))
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
                openVerification = { withdraw ->
                    navController.navigate("withdraw-verification/${withdraw.withdrawId}?steps=${withdraw.twoFactorTypes.joinToString { "${it.code}" }}")
                },
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("withdraw-verification/{withdrawId}?steps={steps}") { backStackEntry ->
            val withdrawId = backStackEntry.arguments?.getString("withdrawId") ?: return@composablePage
            val steps: List<Int> = backStackEntry.arguments?.getString("steps")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()

            Log.e("e", "withdraw-verification: withdrawId=$withdrawId, steps= ${steps.joinToString { it.toString() }}")

            val coinzixVerificationViewModel = viewModel<CoinzixVerificationViewModel>(
                factory = CoinzixVerificationViewModel.FactoryForWithdraw(
                    withdrawId,
                    steps.mapNotNull { TwoFactorType.fromCode(it) },
                    coinzixCexProvider
                )
            )
            val view = LocalView.current
            CoinzixVerificationScreen(
                viewModel = coinzixVerificationViewModel,
                onSuccess = {
                    HudHelper.showSuccessMessage(view, R.string.CexWithdraw_WithdrawSuccess)
                    fragmentNavController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() }
            )

        }
    }
}
