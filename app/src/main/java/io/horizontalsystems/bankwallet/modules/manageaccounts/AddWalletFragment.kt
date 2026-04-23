package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.balance.ui.AddWalletView
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class AddWalletFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        AddWalletScreen(navController, input)
    }
}

@Composable
fun AddWalletScreen(navController: NavController, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_AddWallet),
        onBack = navController::popBackStack,
    ) {
        AddWalletView(
            onNewWalletClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.createAccountFragment, input)

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.NewWallet))
                }
            },
            onWalletRestoreClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.importWalletFragment, input)

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ImportWallet))
                }
            },
            onWatchWalletClick = {
                navController.slideFromRight(R.id.watchAddressFragment, input)

                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
            }
        )
    }
}