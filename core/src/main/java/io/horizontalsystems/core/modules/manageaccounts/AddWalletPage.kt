package io.horizontalsystems.core.modules.manageaccounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.NavigationType
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.modules.balance.ui.AddWalletView
import io.horizontalsystems.core.modules.createaccount.CreateAccountPage
import io.horizontalsystems.core.modules.importwallet.ImportWalletPage
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.watchaddress.WatchAddressPage
import io.horizontalsystems.core.uiv3.components.HSScaffold
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