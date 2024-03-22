package io.horizontalsystems.bankwallet.modules.swapxxx.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class ActionRevoke(
    private val token: Token,
    private val sendEvmData: SendEvmData,
    override val inProgress: Boolean,
    private val allowance: BigDecimal
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Swap_Revoke)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Swap_Revoking)

    @Composable
    override fun getDescription() =
        stringResource(R.string.Approve_RevokeAndApproveInfo, CoinValue(token, allowance).getFormattedFull())

    override fun execute(navController: NavController, onActionCompleted: () -> Unit) {
        navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
            R.id.swapApproveConfirmationFragment,
            SwapApproveConfirmationModule.Input(sendEvmData, token.blockchainType, false)
        ) {
            onActionCompleted.invoke()
        }
    }
}
