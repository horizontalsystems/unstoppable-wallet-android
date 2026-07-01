package io.horizontalsystems.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveConfirmPage
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApprovePage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
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
    override fun executor(navigation: HSNavigation, onActionCompleted: () -> Unit): () -> Unit {
        val approveData = Eip20ApprovePage.Input(
            tokenIn,
            requiredAllowance,
            spenderAddress
        )

        return navigation.slideFromBottomForResult<Eip20ApproveConfirmPage.Result>(
            { Eip20ApprovePage(approveData) }
        ) {
            onActionCompleted.invoke()
        }
    }
}
