package cash.p.terminal.modules.multiswap.action

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R

class ActionCreate(
    override val inProgress: Boolean,
    @StringRes val descriptionResId: Int,
    private val onActionExecuted: (onActionCompleted: () -> Unit) -> Unit
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.swap_create_wallets)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.swap_creating_wallets)

    @Composable
    override fun getDescription() = stringResource(descriptionResId)

    override fun execute(navController: NavController, onActionCompleted: () -> Unit) {
        onActionExecuted(onActionCompleted)
    }
}
