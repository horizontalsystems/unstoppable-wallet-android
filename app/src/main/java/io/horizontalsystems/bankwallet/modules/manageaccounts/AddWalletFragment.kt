package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.balance.ui.AddWalletView
import io.horizontalsystems.bankwallet.modules.createaccount.CreateAccountFragment
import io.horizontalsystems.bankwallet.modules.importwallet.ImportWalletFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressFragment
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class AddWalletFragment(val input: ManageAccountsModule.Input?) : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        AddWalletScreen(navController, input)
    }
}

@Composable
fun AddWalletScreen(navController: NavBackStack<HSScreen>, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_AddWallet),
        onBack = navController::removeLastOrNull,
    ) {
        AddWalletView(
            onNewWalletClick = {
                navController.navigateWithTermsAccepted(
                    screen = CreateAccountFragment(input),
                    navigationType = NavigationType.SlideFromRight,
                    statPageFrom = StatPage.Balance,
                    statPageTo = StatPage.NewWallet
                )
            },
            onWalletRestoreClick = {
                navController.navigateWithTermsAccepted(
                    screen = ImportWalletFragment(input),
                    navigationType = NavigationType.SlideFromRight,
                    statPageFrom = StatPage.Balance,
                    statPageTo = StatPage.ImportWallet
                )
            },
            onWatchWalletClick = {
                navController.slideFromRight(WatchAddressFragment(input))

                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
            }
        )
    }
}