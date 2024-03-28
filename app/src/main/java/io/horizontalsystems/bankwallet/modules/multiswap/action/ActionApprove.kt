package io.horizontalsystems.bankwallet.modules.multiswap.action

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class ActionApprove(
    private val requiredAllowance: BigDecimal,
    private val spenderAddress: Address,
    private val tokenIn: Token,
    private val currentAllowance: BigDecimal,
    override val inProgress: Boolean
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Swap_Unlock)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Swap_Unlocking)

    override fun execute(navController: NavController, onActionCompleted: () -> Unit) {
        val approveData = SwapMainModule.ApproveData(
            tokenIn.blockchainType,
            tokenIn,
            spenderAddress.eip55,
            requiredAllowance,
            currentAllowance
        )

        navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
            R.id.swapApproveFragment,
            approveData
        ) {
            Log.e("AAA", "result: $it")
            onActionCompleted.invoke()
        }
    }
}
