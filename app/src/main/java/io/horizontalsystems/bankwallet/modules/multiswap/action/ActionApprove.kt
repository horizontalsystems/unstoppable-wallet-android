package io.horizontalsystems.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveConfirmFragment
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveFragment
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class ActionApprove(
    private val requiredAllowance: BigDecimal,
    private val spenderAddress: String,
    private val tokenIn: Token,
    override val inProgress: Boolean
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Swap_Unlock)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Swap_Unlocking)

    override fun execute(navController: NavController, onActionCompleted: () -> Unit) {
        val approveData = Eip20ApproveFragment.Input(
            tokenIn,
            requiredAllowance,
            spenderAddress
        )

        navController.slideFromBottomForResult<Eip20ApproveConfirmFragment.Result>(
            R.id.eip20ApproveFragment,
            approveData
        ) {
            onActionCompleted.invoke()
        }
    }
}
