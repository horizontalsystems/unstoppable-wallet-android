package cash.p.terminal.modules.withdrawcex

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
import cash.p.terminal.modules.depositcex.SelectCoinScreen
import cash.p.terminal.ui.compose.ComposeAppTheme

class WithdrawCexChooseAssetFragment : BaseFragment() {

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
                WithdrawCexChooseAssetScreen(findNavController())
            }
        }
    }

}

@Composable
fun WithdrawCexChooseAssetScreen(navController: NavController) {
    ComposeAppTheme {
        SelectCoinScreen(
            onClose = { navController.popBackStack() },
            itemIsSuspended = { !it.withdrawEnabled },
            openNetworkSelect = { cexAsset ->
                navController.slideFromRight(R.id.withdrawCexFragment, WithdrawCexFragment.args(cexAsset))
            },
            withBalance = true
        )
    }
}
