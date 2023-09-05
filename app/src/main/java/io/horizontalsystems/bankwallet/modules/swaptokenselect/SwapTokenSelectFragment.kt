package cash.p.terminal.modules.swaptokenselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.findNavController

class SwapTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
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