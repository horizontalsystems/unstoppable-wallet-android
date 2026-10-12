package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.chain.SpendPurpose
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

/**
 * Shields the transparent balance so a send or swap can spend it. In progress until the
 * shielded funds are spendable, not just until the shielding is broadcast.
 */
class ActionShield(
    private val wallet: Wallet,
    private val purpose: SpendPurpose,
    override val inProgress: Boolean,
    private val confirmations: Int? = null,
    private val requiredConfirmations: Int = 0,
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = when (purpose) {
        SpendPurpose.Send -> stringResource(R.string.Zcash_ShieldToSend)
        SpendPurpose.Swap -> stringResource(R.string.Zcash_ShieldToSwap)
    }

    @Composable
    override fun getTitleInProgress() = if (confirmations != null) {
        stringResource(R.string.Zcash_ShieldingProgress, confirmations, requiredConfirmations)
    } else {
        stringResource(R.string.Zcash_Shielding)
    }

    @Composable
    override fun executor(navigation: HSNavigation, onActionCompleted: () -> Unit): () -> Unit {
        return navigation.slideFromRightForResult<ShieldZcashPage.Result>(
            {
                ShieldZcashPage(
                    ShieldZcashPage.Input(
                        wallet = wallet,
                        entryPointDestId = ShieldZcashPage::class,
                        returnsResult = true,
                    )
                )
            }
        ) {
            onActionCompleted.invoke()
        }
    }
}
