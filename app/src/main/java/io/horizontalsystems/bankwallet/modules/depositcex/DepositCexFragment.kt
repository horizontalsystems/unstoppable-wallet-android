package cash.p.terminal.modules.depositcex

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
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.settings.about.*
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*

class DepositCexFragment : BaseFragment() {

    companion object {
        fun args(coinUid: String): Bundle {
            return bundleOf("coinUid" to coinUid)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val coinUid = arguments?.getString("coinUid")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    DepositCexNavHost(findNavController(), coinUid)
                }
            }
        }
    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DepositCexNavHost(
    fragmentNavController: NavController,
    coinUid: String?,
) {
    val depositViewModel: DepositViewModel = viewModel(factory = DepositCexModule.Factory(coinUid))
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "select-network",
    ) {
        composablePage("select-network") {
            SelectNetworkScreen(
                coinUid = coinUid,
                depositViewModel = depositViewModel,
                openCoinSelect = { navController.navigate("select-coin") },
                openQrCode = { navController.navigate("deposit-qrcode") },
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("select-coin") {
            SelectCoinScreen(
                depositViewModel = depositViewModel,
                onClose = { fragmentNavController.popBackStack() },
                openNetworkSelect = { coinUid ->
                    navController.navigate("select-network")
                },
            )
        }
        composablePage("deposit-qrcode") {
            DepositQrCodeScreen(
                depositViewModel = depositViewModel,
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
            )
        }
    }
}
