package cash.p.terminal.modules.swaptokenselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.findNavController

class SwapTokenSelectFragment : BaseFragment() {
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
                val navController = findNavController()
                TokenSelectScreen(
                    navController = navController,
                    title = stringResource(R.string.Balance_Swap),
                    onClickEnabled = { it.swapEnabled },
                    onClickItem = {
                        navController.slideFromRight(
                            R.id.swapFragment,
                            SwapMainModule.prepareParams(it.wallet.token, R.id.swapTokenSelectFragment)
                        )
                    },
                    viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSwap()),
                    emptyItemsText = stringResource(R.string.Balance_NoAssetsToSwap)
                )
            }
        }
    }
}