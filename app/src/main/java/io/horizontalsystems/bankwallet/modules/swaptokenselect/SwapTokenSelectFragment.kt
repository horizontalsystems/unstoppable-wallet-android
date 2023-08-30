package io.horizontalsystems.bankwallet.modules.swaptokenselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
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
                        navController.slideFromBottom(
                            R.id.swapFragment,
                            SwapMainModule.prepareParams(it.wallet.token)
                        )
                    },
                    viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSwap()),
                    emptyItemsText = stringResource(R.string.Balance_NoAssetsToSwap)
                )
            }
        }
    }
}