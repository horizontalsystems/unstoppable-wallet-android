package io.horizontalsystems.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveConfirmFragment
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class ActionApprove(
    private val requiredAllowance: BigDecimal,
    private val spenderAddress: String,
    private val tokenIn: Token,
    override val inProgress: Boolean
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Swap_Approve)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Swap_Approving)

    @Composable
    override fun executor(navController: NavBackStack<HSScreen>, onActionCompleted: () -> Unit): () -> Unit {
        val approveData = Eip20ApproveFragment.Input(
            tokenIn,
            requiredAllowance,
            spenderAddress
        )

        return navController.slideFromBottomForResult<Eip20ApproveConfirmFragment.Result>(
            Eip20ApproveFragment(approveData)
        ) {
            onActionCompleted.invoke()
        }
    }
}
