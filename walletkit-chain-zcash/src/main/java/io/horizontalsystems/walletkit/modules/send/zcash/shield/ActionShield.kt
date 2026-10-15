package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashSpendStep
import io.horizontalsystems.walletkit.core.chain.SpendPurpose
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

/**
 * The [ZcashSpendStep] a send or swap form shows in place of its button: Shield opens the
 * shield confirmation; Shielding and Confirming keep the form waiting until the funds are
 * spendable.
 */
class ActionShield(
    private val wallet: Wallet,
    private val purpose: SpendPurpose,
    private val step: ZcashSpendStep,
) : ISwapProviderAction {

    override val inProgress: Boolean
        get() = step != ZcashSpendStep.Shield

    @Composable
    override fun getTitle() = when (purpose) {
        SpendPurpose.Send -> stringResource(R.string.Zcash_ShieldToSend)
        SpendPurpose.Swap -> stringResource(R.string.Zcash_ShieldToSwap)
    }

    @Composable
    override fun getTitleInProgress() = when (step) {
        ZcashSpendStep.Confirming -> stringResource(R.string.Zcash_Confirming)
        else -> stringResource(R.string.Zcash_Shielding)
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
