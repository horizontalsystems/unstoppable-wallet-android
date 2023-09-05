package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class DepositCexChooseAssetFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        DepositCexChooseAssetScreen(findNavController())
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
