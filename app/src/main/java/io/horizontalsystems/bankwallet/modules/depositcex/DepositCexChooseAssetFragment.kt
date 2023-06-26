package cash.p.terminal.modules.depositcex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.ui.compose.ComposeAppTheme

class DepositCexChooseAssetFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                DepositCexFragmentChooseAssetScreen(findNavController())
            }
        }
    }
}

@Composable
fun DepositCexFragmentChooseAssetScreen(navController: NavController) {
    ComposeAppTheme {
        SelectCoinScreen(
            onClose = { navController.popBackStack() },
            openNetworkSelect = { cexAsset ->
                navController.slideFromRight(R.id.depositCexFragment, DepositCexFragment.args(cexAsset))
            },
        )
    }
}
