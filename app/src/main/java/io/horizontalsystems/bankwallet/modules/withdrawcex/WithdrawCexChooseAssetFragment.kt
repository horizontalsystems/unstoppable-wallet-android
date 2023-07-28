package io.horizontalsystems.bankwallet.modules.withdrawcex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.depositcex.SelectCoinScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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
            onSelectAsset = { cexAsset ->
                navController.popBackStack()

                navController.slideFromRight(R.id.withdrawCexFragment, WithdrawCexFragment.args(cexAsset))
            },
            withBalance = true
        )
    }
}
