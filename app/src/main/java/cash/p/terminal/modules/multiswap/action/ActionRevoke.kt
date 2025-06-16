package cash.p.terminal.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import io.horizontalsystems.core.slideFromBottomForResult
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.eip20revoke.Eip20RevokeConfirmFragment
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

class ActionRevoke(
    private val token: Token,
    private val spenderAddress: String,
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
        navController.slideFromBottomForResult<Eip20RevokeConfirmFragment.Result>(
            R.id.eip20RevokeConfirmFragment,
            Eip20RevokeConfirmFragment.Input(token, spenderAddress, allowance)
        ) {
            onActionCompleted.invoke()
        }
    }
}
