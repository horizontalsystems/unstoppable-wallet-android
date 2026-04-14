package com.quantum.wallet.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.slideFromBottomForResult
import com.quantum.wallet.bankwallet.modules.eip20approve.Eip20ApproveConfirmFragment
import com.quantum.wallet.bankwallet.modules.eip20approve.Eip20ApproveFragment
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
