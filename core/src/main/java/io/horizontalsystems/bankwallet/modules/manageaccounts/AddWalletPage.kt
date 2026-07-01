package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.balance.ui.AddWalletView
import io.horizontalsystems.bankwallet.modules.createaccount.CreateAccountPage
import io.horizontalsystems.bankwallet.modules.importwallet.ImportWalletPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressPage
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class AddWalletPage(val input: ManageAccountsModule.Input?) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AddWalletScreen(navigation, input)
    }
}

@Composable
fun AddWalletScreen(navigation: HSNavigation, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_AddWallet),
        onBack = navigation::removeLastOrNull,
    ) {
        AddWalletView(
            onNewWalletClick = {
                navigation.navigateWithTermsAccepted(
                    screen = CreateAccountPage(input),
                    navigationType = NavigationType.SlideFromRight,
                    statPageFrom = StatPage.Balance,
                    statPageTo = StatPage.NewWallet
                )
            },
            onWalletRestoreClick = {
                navigation.navigateWithTermsAccepted(
                    screen = ImportWalletPage(input),
                    navigationType = NavigationType.SlideFromRight,
                    statPageFrom = StatPage.Balance,
                    statPageTo = StatPage.ImportWallet
                )
            },
            onWatchWalletClick = {
                navigation.slideFromRight(WatchAddressPage(input))

                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
            }
        )
    }
}