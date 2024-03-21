package cash.p.terminal.modules.swapxxx.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottomForResult
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import cash.p.terminal.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.marketkit.models.Token

class ActionRevoke(
    private val tokenIn: Token,
    private val sendEvmData: SendEvmData,
    override val inProgress: Boolean
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Swap_Revoke)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Swap_Revoking)

    override fun execute(navController: NavController, onActionCompleted: () -> Unit) {
        navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
            R.id.swapApproveConfirmationFragment,
            SwapApproveConfirmationModule.Input(sendEvmData, tokenIn.blockchainType, false)
        ) {
            onActionCompleted.invoke()
        }
    }
}
