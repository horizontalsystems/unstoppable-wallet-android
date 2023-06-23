package cash.p.terminal.modules.withdrawcex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.modules.settings.about.*
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexConfirmScreen
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexScreen
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexSecurityVerificationScreen
import cash.p.terminal.modules.withdrawcex.ui.WithdrawCexSelectNetworkScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*

class WithdrawCexFragment : BaseFragment() {

    companion object {
        fun args(blockchainType: String): Bundle {
            return bundleOf("blockchain_type" to blockchainType)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val blockchainType = arguments?.getString("blockchain_type")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    WithdrawCexNavHost(findNavController(), blockchainType)
                }
            }
        }
    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WithdrawCexNavHost(
    fragmentNavController: NavController,
    blockchainType: String?,
) {
    val viewModel: WithdrawCexViewModel = viewModel(factory = WithdrawCexModule.Factory())
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "withdraw",
    ) {
        composable("withdraw") {
            WithdrawCexScreen(
                mainViewModel = viewModel,
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
                    navController.navigate("withdraw-verification")
                },
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("withdraw-verification") {
            WithdrawCexSecurityVerificationScreen(
                mainViewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
    }
}
