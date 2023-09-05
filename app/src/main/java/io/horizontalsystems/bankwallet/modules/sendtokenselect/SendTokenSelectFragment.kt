package cash.p.terminal.modules.sendtokenselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.findNavController

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Balance_Send),
            onClickEnabled = { it.sendEnabled },
            onClickItem = {
                navController.slideFromRight(
                    R.id.sendXFragment,
                    SendFragment.prepareParams(it.wallet, R.id.sendTokenSelectFragment)
                )
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

}
