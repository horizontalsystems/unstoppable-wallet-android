package cash.p.terminal.modules.depositcex

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.ui.compose.ComposeAppTheme

class DepositCexChooseAssetFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        DepositCexChooseAssetScreen(navController)
    }

}

@Composable
fun DepositCexChooseAssetScreen(navController: NavController) {
    ComposeAppTheme {
        SelectCoinScreen(
            onClose = { navController.popBackStack() },
            itemIsSuspended = { !it.depositEnabled },
            onSelectAsset = { cexAsset ->
                navController.slideFromRight(R.id.depositCexFragment, DepositCexFragment.args(cexAsset))
            },
            withBalance = false
        )
    }
}
