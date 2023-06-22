package io.horizontalsystems.bankwallet.modules.depositcex

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
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.settings.about.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

class DepositCexFragment : BaseFragment() {

    companion object {
        fun args(assetId: String): Bundle {
            return bundleOf("assetId" to assetId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val assetId = arguments?.getString("assetId")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    DepositCexNavHost(findNavController(), assetId)
                }
            }
        }
    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DepositCexNavHost(
    fragmentNavController: NavController,
    assetId: String?,
) {
    val depositViewModel = viewModel<DepositViewModel>(factory = DepositCexModule.Factory(assetId))
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "select-network",
    ) {
        composablePage("select-network") {
            SelectNetworkScreen(
                assetId = assetId,
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
                openNetworkSelect = { assetId ->
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
